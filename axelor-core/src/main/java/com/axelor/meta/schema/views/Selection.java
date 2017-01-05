/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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

		@XmlAnyAttribute
		private Map<QName, String> data;

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

		public Map<QName, String> getData() {
			return data;
		}
		
		public void setData(Map<QName, String> data) {
			this.data = data;
		}
	}

}
