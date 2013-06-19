package com.axelor.meta.schema.views;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.axelor.meta.MetaStore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("include")
public class FormInclude extends AbstractWidget {
	
	@XmlAttribute(name = "view")
	private String name;

	@XmlAttribute(name = "from")
	private String module;
	
	private transient AbstractView owner;

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getModule() {
		return module;
	}
	
	public void setModule(String module) {
		this.module = module;
	}
	
	public void setOwner(AbstractView owner) {
		this.owner = owner;
	}
	
	@JsonInclude
	public AbstractView getView() {
		AbstractView view = MetaStore.getView(name, module);
		if (view == owner) {
			return null;
		}
		return view;
	}
}
