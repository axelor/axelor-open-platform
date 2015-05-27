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
package com.axelor.meta.schema.views;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.namespace.QName;

import com.axelor.i18n.I18n;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

@XmlType
public class Selection {

	@XmlAttribute
	private String name;
	
	@XmlAttribute(name = "id")
	private String xmlId;

	@XmlElement(name = "option", required = true)
	private List<Selection.Option> options;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getXmlId() {
		return xmlId;
	}
	
	public void setXmlId(String xmlId) {
		this.xmlId = xmlId;
	}

	public List<Selection.Option> getOptions() {
		return options;
	}

	public void setOptions(List<Selection.Option> options) {
		this.options = options;
	}

	@XmlType
	public static class Option {

		@XmlAttribute(required = true)
		private String value;

		@XmlValue
		private String title;

		@XmlAttribute
		private String icon;

		@XmlAttribute
		private Integer order;

		@XmlAttribute
		private Boolean hidden;

		@JsonIgnore
		@XmlAnyAttribute
		private Map<QName, String> dataAttributes;

		@XmlTransient
		private Map<String, Object> data;

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		@JsonGetter("title")
		public String getLocalizedTitle(){
			return I18n.get(title);
		}

		@JsonIgnore
		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getIcon() {
			return icon;
		}

		public void setIcon(String icon) {
			this.icon = icon;
		}

		public Integer getOrder() {
			return order;
		}

		public void setOrder(Integer order) {
			this.order = order;
		}

		public Boolean getHidden() {
			return hidden;
		}

		public void setHidden(Boolean hidden) {
			this.hidden = hidden;
		}

		public Map<QName, String> getDataAttributes() {
			return dataAttributes;
		}

		@JsonGetter
		public Map<String, Object> getData() {
			return data;
		}

		public void setData(Map<String, Object> data) {
			this.data = data;
		}
	}
}
