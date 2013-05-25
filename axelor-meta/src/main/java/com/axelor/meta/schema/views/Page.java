package com.axelor.meta.schema.views;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("page")
public class Page extends SimpleContainer {
	
	@XmlAttribute
	private String icon;
	
	@XmlAttribute
	private String onSelect ;

	public String getIcon() {
		return icon;
	}

	public String getOnSelect() {
		return onSelect;
	}

	public void setOnSelect(String onSelect) {
		this.onSelect = onSelect;
	}

}
