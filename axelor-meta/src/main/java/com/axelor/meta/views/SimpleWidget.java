package com.axelor.meta.views;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlTransient
public abstract class SimpleWidget extends AbstractWidget {

	@XmlAttribute
	private String name;
	
	@XmlAttribute
	private String title;
	
	@XmlAttribute
	private String help;
	
	@XmlAttribute
	private Boolean noLabel;
	
	@XmlAttribute
	private Boolean hidden;
	
	@XmlAttribute
	private Integer colSpan;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getHelp() {
		return help;
	}

	public void setHelp(String help) {
		this.help = help;
	}

	public Boolean getNoLabel() {
		return noLabel;
	}
	
	public void setNoLabel(Boolean noLabel) {
		this.noLabel = noLabel;
	}
	
	public Boolean getHidden() {
		return hidden;
	}
	
	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}

	public Integer getColSpan() {
		return colSpan;
	}
	
	public void setColSpan(Integer colSpan) {
		this.colSpan = colSpan;
	}
}
