/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.meta.schema.actions.validate;

import com.axelor.common.StringUtils;
import com.axelor.i18n.I18n;
import com.axelor.meta.ActionHandler;
import com.axelor.meta.schema.actions.ActionResumable;
import com.axelor.meta.schema.actions.validate.validator.Alert;
import com.axelor.meta.schema.actions.validate.validator.Error;
import com.axelor.meta.schema.actions.validate.validator.Info;
import com.axelor.meta.schema.actions.validate.validator.Notify;
import com.axelor.meta.schema.actions.validate.validator.Validator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class ActionValidate extends ActionResumable {

  @JsonIgnore
  @XmlElements({
    @XmlElement(name = "error", type = Error.class),
    @XmlElement(name = "alert", type = Alert.class),
    @XmlElement(name = "info", type = Info.class),
    @XmlElement(name = "notify", type = Notify.class)
  })
  private List<Validator> validators;

  public List<Validator> getValidators() {
    return validators;
  }

  public void setValidators(List<Validator> validators) {
    this.validators = validators;
  }

  @Override
  protected ActionValidate copy() {
    final ActionValidate action = new ActionValidate();
    final List<Validator> items = new ArrayList<>(validators);
    action.setName(getName());
    action.setModel(getModel());
    action.setValidators(items);
    return action;
  }

  @Override
  public Object evaluate(ActionHandler handler) {

    Map<String, String> info = null;
    final List<Map<String, String>> notify = Lists.newArrayList();
    final Map<String, Object> result = Maps.newHashMap();

    for (int i = getIndex(); i < validators.size(); i++) {

      final Validator validator = validators.get(i);
      if (!validator.test(handler)) {
        continue;
      }

      String key = validator.getKey();
      String message = I18n.get(validator.getMessage());

      if (!StringUtils.isBlank(message)) {
        message = handler.evaluate(toExpression(message, true)).toString();
      }

      Map<String, String> value = validator.toMap(message);

      if (validator instanceof Info) {
        // Only displays the first `info` in case of multiple matches
        info = info == null ? value : info;
        continue;
      }
      if (validator instanceof Notify) {
        notify.add(value);
        continue;
      }

      result.put(key, value);

      if (i + 1 < validators.size() && validator instanceof Alert) {
        result.put("pending", String.format("%s[%d]", getName(), i + 1));
      }

      if (info != null) {
        result.put(Info.KEY, info);
      }
      if (!notify.isEmpty()) {
        result.put(Notify.KEY, notify);
      }

      return result;
    }

    if (info != null) {
      result.put(Info.KEY, info);
    }
    if (!notify.isEmpty()) {
      result.put(Notify.KEY, notify);
    }

    return result.isEmpty() ? null : result;
  }
}
