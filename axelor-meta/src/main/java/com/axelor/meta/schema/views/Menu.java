package com.axelor.meta.schema.views;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.axelor.db.JPA;

@XmlType
public class Menu {

	@XmlAttribute
	private String title;

	@XmlAttribute
	private String icon;

	@XmlAttribute
	private Boolean showTitle;

	@XmlElements({ @XmlElement(name = "item"), @XmlElement(name = "divider") })
	private List<MenuItem> items;

	public String getDefaultTitle() {
		return title;
	}

	public String getTitle() {
		return JPA.translate(title);
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

	public Boolean getShowTitle() {
		return showTitle;
	}

	public void setShowTitle(Boolean showTitle) {
		this.showTitle = showTitle;
	}

	public List<MenuItem> getItems() {
		return items;
	}

	public void setItems(List<MenuItem> items) {
		this.items = items;
	}
}