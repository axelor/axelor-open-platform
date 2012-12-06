package com.axelor.meta.views;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@XmlType
@JsonTypeInfo(use = Id.NONE)
public class Portlet extends AbstractContainer {

	private String resource;

	private String domain;
	
	@XmlElement(name = "view-name")
	private String viewName;
	
	@XmlElement(name = "view-type")
	private String viewType;

	public String getResource() {
		return resource;
	}
	
	public void setResource(String resource) {
		this.resource = resource;
	}
	
	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}
	
	public String getViewName() {
		return viewName;
	}
	
	public void setViewName(String viewName) {
		this.viewName = viewName;
	}
	
	public String getViewType() {
		return viewType;
	}
	
	public void setViewType(String viewType) {
		this.viewType = viewType;
	}

}
