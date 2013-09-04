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