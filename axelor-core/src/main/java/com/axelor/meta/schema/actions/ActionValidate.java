/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.axelor.common.StringUtils;
import com.axelor.i18n.I18n;
import com.axelor.meta.ActionHandler;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@XmlType
public class ActionValidate extends ActionResumable {

	public static class Validator extends Element {

		@XmlAttribute(name = "message")
		private String message;

		@XmlAttribute(name = "action")
		private String action;

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public String getAction() {
			return action;
		}

		public void setAction(String action) {
			this.action = action;
		}
	}

	@XmlType
	public static class Error extends Validator {
	}

	@XmlType
	public static class Alert extends Validator {
	}

	@XmlType
	public static class Info extends Validator {
	}
	
	@XmlType
	public static class Notify extends Validator {
	}

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

		final List<String> info = Lists.newArrayList();
		final List<String> notify = Lists.newArrayList();
		final Map<String, Object> result = Maps.newHashMap();

		for (int i = getIndex(); i < validators.size(); i++) {

			final Validator validator = validators.get(i);
			if (!validator.test(handler)) {
				continue;
			}

			String key = validator.getClass().getSimpleName().toLowerCase();
			String value = I18n.get(validator.getMessage());

			if (!StringUtils.isBlank(value)) {
				value = handler.evaluate("eval: " + "\"\"\"" + value + "\"\"\"").toString();
			}

			if (validator instanceof Info) {
				info.add(value);
				continue;
			}
			if (validator instanceof Notify) {
				notify.add(value);
				continue;
			}

			result.put(key, value);

			if (!StringUtils.isBlank(validator.getAction())) {
				result.put("action", validator.getAction());
			}
			
			if (i + 1 < validators.size() && validator instanceof Alert) {
				result.put("pending", String.format("%s[%d]", getName(), i + 1));
			}

			if (!info.isEmpty()) {
				result.put("info", info);
			}
			if (!notify.isEmpty()) {
				result.put("notify", notify);
			}

			return result;
		}

		if (!info.isEmpty()) {
			result.put("info", info);
		}
		if (!notify.isEmpty()) {
			result.put("notify", notify);
		}

		return result.isEmpty() ? null : result;
	}

	@Override
	public Object wrap(ActionHandler handler) {
		return evaluate(handler);
	}
}