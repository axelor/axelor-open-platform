/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
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
 * Software distributed under the License is distributed on an “AS IS”
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
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
package com.axelor.meta.schema.actions;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.axelor.db.JPA;
import com.axelor.meta.ActionHandler;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

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
		boolean allCheck = true;
		for(Check check : conditions) {
			String names = check.getField();
			String error = check.getError();
			if (Strings.isNullOrEmpty(names)
					&& Strings.isNullOrEmpty(error)
					&& !check.test(handler)) {
				return false;
			}
			if (names == null) {
				continue;
			}
			allCheck = false;
			for (String field : names.split(",")) {
				field = field.trim();
				if (check.test(handler, check.getCondition(field))) {
					errors.put(field, error);
				}
			}

		}
		return allCheck ? true : errors;
	}

	@Override
	public Object wrap(ActionHandler handler) {
		Object value = evaluate(handler);
		if (value instanceof Map) {
			return ImmutableMap.of("errors", value);
		}
		return value;
	}

	@XmlType
	public static class Check extends Element {

		@XmlAttribute
		private String field;

		@XmlAttribute
		private String error;

		public String getCondition(String field) {
			String condition = this.getCondition();
			if (isEmpty(condition) && !isEmpty(field)) {
				return field + " == null";
			}
			return condition != null ? condition.trim() : condition;
		}

		public String getField() {
			return field;
		}

		public String getDefaultError() {
			return error;
		}

		public String getError() {
			if (isEmpty(error)) {
				if (isEmpty(this.getCondition())) {
					return JPA.translate("Field is required.", "Field is required.", null, "action");
				}
				return JPA.translate("Invalid field value.", "Invalid field value.", null, "action");
			}
			return JPA.translate(error, error, null, "action");
		}

		private boolean isEmpty(String str) {
			return str == null || "".equals(str.trim());
		}

		@Override
		public String toString() {
			return Objects.toStringHelper(getClass())
					.add("field", field)
					.add("error", getError())
					.add("condition", getCondition())
					.toString();
		}
	}
}
