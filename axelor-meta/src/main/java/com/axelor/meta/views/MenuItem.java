package com.axelor.meta.views;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.axelor.meta.db.MetaMenu;

@XmlType
public class MenuItem {

	@XmlAttribute
	private String name;
	
	@XmlAttribute
	private String title;
	
	@XmlAttribute
	private String parent;
	
	@XmlAttribute
	private String icon;
	
	@XmlAttribute
	private String action;
	
	@XmlAttribute
	private Integer priority;
	
	@XmlAttribute
	private String groups;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}
	
	public Integer getPriority() {
		return priority;
	}
	
	public void setPriority(Integer priority) {
		this.priority = priority;
	}
	
	public String getGroups() {
		return groups;
	}
	
	public void setGroups(String groups) {
		this.groups = groups;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getAction() {
		return action;
	}
	
	public void setAction(String action) {
		this.action = action;
	}
	
	public Boolean getIsFolder() {
		return MetaMenu.all().filter("self.parent.name = ?1", name).count() > 0;
	}
}
