/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
package com.axelor.meta.schema.actions;

import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.i18n.I18n;
import com.axelor.meta.ActionHandler;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class ActionCondition extends Action {

  @XmlElement(name = "check")
  private List<Check> conditions;

  public List<Check> getConditions() {
    return conditions;
  }

  @Override
  public Object evaluate(ActionHandler handler) {
    Map<String, String> errors = Maps.newHashMap();
    for (Check check : conditions) {
      String names = check.getField();
      String error = check.getLocalizedError();
      if (Strings.isNullOrEmpty(names) && Strings.isNullOrEmpty(error) && !check.test(handler)) {
        return false;
      }
      if (names == null) {
        continue;
      }

      if (!StringUtils.isBlank(error)) {
        error = handler.evaluate(toExpression(error, true)).toString();
      }

      for (String field : names.split(",")) {
        field = field.trim();
        if (Action.test(handler, check.getCondition(field))) {
          errors.put(field, error);
        } else {
          errors.put(field, "");
        }
      }
    }

    return ObjectUtils.isEmpty(errors) ? true : errors;
  }

  @Override
  protected Object wrapper(Object value) {
    final Map<String, Object> result = new HashMap<>();
    if (value instanceof Map) {
      result.put("errors", value);
      return result;
    }
    return value;
  }

  @XmlType
  public static class Check extends Element {

    @XmlAttribute private String field;

    @XmlAttribute private String error;

    public String getCondition(String field) {
      String condition = this.getCondition();
      if (StringUtils.isBlank(condition) && !StringUtils.isBlank(field)) {
        return field + " == null";
      }
      return condition != null ? condition.trim() : condition;
    }

    public String getField() {
      return field;
    }

    public String getError() {
      return error;
    }

    public String getLocalizedError() {
      if (StringUtils.isBlank(error)) {
        if (StringUtils.isBlank(this.getCondition())) {
          return I18n.get("Field is required.");
        }
        return I18n.get("Invalid field value.");
      }
      return I18n.get(error);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(getClass())
          .add("field", field)
          .add("error", getError())
          .add("condition", getCondition())
          .toString();
    }
  }
}
