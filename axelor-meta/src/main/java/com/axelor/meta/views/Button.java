package com.axelor.meta.views;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.axelor.db.JPA;
import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("button")
public class Button extends SimpleWidget {

	@XmlAttribute
	private String prompt;
	
	@XmlAttribute
	private String onClick;
	
	public String getPrompt() {
		return JPA.translate(prompt);
	}
	
	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}

	public String getOnClick() {
		return onClick;
	}

	public void setOnClick(String onClick) {
		this.onClick = onClick;
	}
}
