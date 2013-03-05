package com.axelor.meta.views;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.axelor.meta.ActionHandler;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

@XmlType
public class ActionCondition extends Action {

	@XmlElement(name = "check")
	private List<Check> conditions;
	
	@Override
	public Object evaluate(ActionHandler handler) {
		Map<String, String> errors = Maps.newHashMap();
		for(Check check : conditions) {
			for (String field : check.getField().split(",")) {
				field = field.trim();
				if (check.test(handler,check.getCondition(field))) {
					errors.put(field, check.getError());
				}
			}
			
		}
		return errors;
	}

	@Override
	public Object wrap(ActionHandler handler) {
		return ImmutableMap.of("errors", evaluate(handler));
	}
	
	@XmlType
	public static class Check extends Act {
		
		@XmlAttribute
		private String field;
		
		@XmlAttribute
		private String error;
		
		public String getCondition(String field) {
			String condition = super.getCondition();
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
				if (isEmpty(super.getCondition())) {
					return "Field is required.";
				}
				return "Invalid field value.";
			}
			return error;
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
