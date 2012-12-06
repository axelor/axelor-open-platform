package com.axelor.meta.views;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("group")
public class Group extends SimpleContainer {

	@XmlAttribute
	private Boolean canCollapse;
	
	public Boolean getCanCollapse() {
		return canCollapse;
	}
	
	public void setCanCollapse(Boolean canCollapse) {
		this.canCollapse = canCollapse;
	}
}
