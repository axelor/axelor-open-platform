package com.axelor.meta.schema.views;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("portlet")
public class Portlet extends AbstractContainer {

	@XmlAttribute
	private String action;
	
	@XmlAttribute
	private Boolean canSearch;
	
	public String getAction() {
		return action;
	}
	
	public void setAction(String action) {
		this.action = action;
	}
	
	public Boolean getCanSearch() {
		return canSearch;
	}
	
	public void setCanSearch(Boolean canSearch) {
		this.canSearch = canSearch;
	}
}
