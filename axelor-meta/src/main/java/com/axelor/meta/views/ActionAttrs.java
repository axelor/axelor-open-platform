package com.axelor.meta.views;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.axelor.meta.ActionHandler;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

@XmlType
public class ActionAttrs extends Action {

	@XmlType
	public static class Attribute extends Element {
	
		@XmlAttribute(name = "for")
		private String fieldName;
		
		public String getFieldName() {
			return fieldName;
		}
		
		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}
	}

	@XmlElement(name = "attribute", type = Attribute.class)
	private List<Attribute> attributes;

	public List<Attribute> getAttributes() {
		return attributes;
	}
	
	public void setAttributes(List<Attribute> attributes) {
		this.attributes = attributes;
	}
	
	@Override @SuppressWarnings("all")
	public Object evaluate(ActionHandler handler) {
		
		Map<String, Object> map = Maps.newHashMap();
		for(Attribute attribute : attributes) {
			if (!attribute.test(handler) || Strings.isNullOrEmpty(attribute.getFieldName())) continue;
			for(String field : attribute.fieldName.split(",")){
				if (Strings.isNullOrEmpty(field)) { continue; }
				field = field.trim();
				Map<String, Object> attrs = (Map) map.get(field);
				if (attrs == null) {
					attrs = Maps.newHashMap();
					map.put(field, attrs);
				}
				
				String name = attribute.getName();
				Object value = null;
				if (name.matches("readonly|required|recommend|hidden|collapse")) {
					value = attribute.test(handler, attribute.getExpression());
				} else {
					value = handler.evaluate(attribute.getExpression());
				}
				attrs.put(attribute.getName(), value);
			}
		}
		return map;
	}
	
	@Override
	public Object wrap(ActionHandler handler) {
		Object value = evaluate(handler);
		if (value == null) {
			return null;
		}
		return ImmutableMap.of("attrs", value);
	}
}