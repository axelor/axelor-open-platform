package com.axelor.meta.views;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlType
public class Selection {
	
	@XmlAttribute
	private String name;
	
	@XmlElement(name = "option", required = true)
	private List<Selection.Option> options;

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
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

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	
	}

}
