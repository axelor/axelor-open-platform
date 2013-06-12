package com.axelor.meta.domains;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(name = "domain-models")
public class DomainModels {
	
	public static final String NAMESPACE = "http://apps.axelor.com/xml/ns/domain-models";

	public static final String VERSION = "0.9";
	
	@XmlElement(name = "module")
	private Module module;

	@XmlElement(name = "entity")
	private List<Entity> entities;

	public Module getModule() {
		return module;
	}

	public void setModule(Module module) {
		this.module = module;
	}

	public List<Entity> getEntities() {
		return entities;
	}

	public void setEntities(List<Entity> entities) {
		this.entities = entities;
	}

	
}
