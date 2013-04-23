package com.axelor.meta.views;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.axelor.db.JPA;

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
	private Boolean readonly;
	
	@XmlAttribute
	private Integer colSpan;
	
	@XmlAttribute
	private String css;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getTitle() {
		return getTranslations();
	}
	
	public String getDefaultTitle() {
		return title;
	}
	
	protected String getTranslations() {
		return JPA.translate(title);
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getHelp() {
		return JPA.translate(help);
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
	
	public Boolean getReadonly() {
		return readonly;
	}
	
	public void setReadonly(Boolean readonly) {
		this.readonly = readonly;
	}

	public Integer getColSpan() {
		return colSpan;
	}
	
	public void setColSpan(Integer colSpan) {
		this.colSpan = colSpan;
	}
	
	public String getCss() {
		return css;
	}
	
	public void setCss(String css) {
		this.css = css;
	}
}
