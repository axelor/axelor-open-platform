package com.axelor.meta.views;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.axelor.db.JPA;
import com.axelor.meta.db.MetaView;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonInclude(Include.NON_NULL)
@JsonSubTypes({
	@Type(GridView.class),
	@Type(FormView.class),
	@Type(TreeView.class),
	@Type(Portal.class),
	@Type(Search.class)
})
public abstract class AbstractView {

	@XmlAttribute
	private String name;
	
	@XmlAttribute
	private String title;
	
	@XmlAttribute
	private String model;
	
	@XmlAttribute
	private Boolean editable;
	
	@XmlElementWrapper
	@XmlElement(name = "button")
	private List<Button> toolbar;

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getTitle() {
		return JPA.translate(title);
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getModel() {
		if(model != null)
			return model;
		
		MetaView view = MetaView.all().filter("self.name = ?1", name).fetchOne();
		if(view != null && view.getModel() != null){
			model = view.getModel();
		}
		
		return model;
	}
	
	public void setModel(String model) {
		this.model = model;
	}
	
	public Boolean getEditable() {
		return editable;
	}
	
	public void setEditable(Boolean editable) {
		this.editable = editable;
	}
	
	public List<Button> getToolbar() {
		return toolbar;
	}
	
	public void setToolbar(List<Button> toolbar) {
		this.toolbar = toolbar;
	}
	
	@XmlTransient
	public String getType() {
		try {
			return getClass().getAnnotation(JsonTypeName.class).value();
		} catch(Exception e){}
		return "unknown";
	}
}
