/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.axelor.common.StringUtils;
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

			if (!StringUtils.isBlank(error)) {
				error = handler.evaluate("eval: " + "\"\"\"" + error + "\"\"\"").toString();
			}

			for (String field : names.split(",")) {
				field = field.trim();
				if (Action.test(handler, check.getCondition(field))) {
					errors.put(field, error);
					allCheck = false;
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
