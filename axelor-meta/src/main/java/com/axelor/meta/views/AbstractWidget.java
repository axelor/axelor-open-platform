package com.axelor.meta.views;

import java.util.Map;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@XmlType
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type", defaultImpl = Field.class)
@JsonInclude(Include.NON_NULL)
@JsonSubTypes({
	@Type(Field.class),
	@Type(Button.class),
	@Type(Break.class),
	@Type(Spacer.class),
	@Type(Separator.class),
	@Type(Label.class),
	@Type(Group.class),
	@Type(Notebook.class),
	@Type(Page.class),
	@Type(Portlet.class)
})
public abstract class AbstractWidget {

	@XmlAnyAttribute
	private Map<QName, String> otherAttributes;
	
	private String translationModel;
	
	public Map<QName, String> getOtherAttributes() {
		return otherAttributes;
	}
	
	public void setOtherAttributes(Map<QName, String> otherAttributes) {
		this.otherAttributes = otherAttributes;
	}

	public String getTranslationModel() {
		return translationModel;
	}

	public void setTranslationModel(String translationModel) {
		this.translationModel = translationModel;
	}
}
