/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.meta.schema.actions;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.meta.ActionHandler;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@XmlType
public class ActionValidate extends ActionIndex {

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

	@JsonIgnore
	@XmlElements({
		@XmlElement(name = "error", type = Error.class),
		@XmlElement(name = "alert", type = Alert.class),
		@XmlElement(name = "info", type = Info.class),
	})
	private List<Validator> validators;

	public List<Validator> getValidators() {
		return validators;
	}

	public void setValidators(List<Validator> validators) {
		this.validators = validators;
	}

	@Override
	public Object evaluate(ActionHandler handler) {

		final List<String> info = Lists.newArrayList();
		final Map<String, Object> result = Maps.newHashMap();

		for (int i = getIndex(); i < validators.size(); i++) {

			final Validator validator = validators.get(i);
			if (!validator.test(handler)) {
				continue;
			}

			String key = validator.getClass().getSimpleName().toLowerCase();
			String value = JPA.translate(validator.getMessage(), validator.getMessage(), null, "action");

			if (!StringUtils.isBlank(value)) {
				value = handler.evaluate("eval: " + "\"\"\"" + value + "\"\"\"").toString();
			}

			if (validator instanceof Info) {
				info.add(value);
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

			return result;
		}
		
		if (info.isEmpty()) {
			return null;
		}

		result.put("info", info);
		return result;
	}

	@Override
	public Object wrap(ActionHandler handler) {
		return evaluate(handler);
	}
}