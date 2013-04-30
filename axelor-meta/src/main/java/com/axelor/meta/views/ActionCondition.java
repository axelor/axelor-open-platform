package com.axelor.meta.views;

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
	public static class Check extends Act {
		
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
		
		public String getError() {
			if (isEmpty(error)) {
				if (isEmpty(this.getCondition())) {
					return JPA.translate("Field is required.");
				}
				return JPA.translate("Invalid field value.");
			}
			return JPA.translate(error);
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
