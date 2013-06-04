package com.axelor.meta.schema.actions;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.axelor.db.JPA;
import com.axelor.meta.ActionHandler;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

@XmlType
public class ActionValidate extends Action {
	
	@XmlType
	public static class Validator extends Element {
		
		@XmlAttribute(name = "message")
		private String message;
		
		public String getMessage() {
			return message;
		}
		
		public void setMessage(String message) {
			this.message = message;
		}
	}
	
	@JsonIgnore
	@XmlElements({
		@XmlElement(name = "error", type = Validator.class),
		@XmlElement(name = "alert", type = Validator.class),
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
		for(Validator validator : validators) {
			if (validator.test(handler)) {
				String key = validator.getClass().getSimpleName().toLowerCase();
				String val = JPA.translate(validator.getMessage());
				
				if (!Strings.isNullOrEmpty(val))
					val = handler.evaluate("eval: " + "\"\"\"" + val + "\"\"\"").toString();
				
				Map<String, Object> result = Maps.newHashMap();
				result.put(key, val);
				return result;
			}
		}
		return null;
	}
	
	@Override
	public Object wrap(ActionHandler handler) {
		return evaluate(handler);
	}
}