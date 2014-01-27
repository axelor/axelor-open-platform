/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.meta.schema.views;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.axelor.db.JPA;
import com.axelor.meta.db.MetaView;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Strings;

@XmlType
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonInclude(Include.NON_NULL)
@JsonSubTypes({
	@Type(GridView.class),
	@Type(FormView.class),
	@Type(TreeView.class),
	@Type(ChartView.class),
	@Type(CalendarView.class),
	@Type(Portal.class),
	@Type(Search.class),
	@Type(SearchFilters.class)
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

	@JsonIgnore
	@XmlAttribute( name = "width")
	private String widthSpec;

	@XmlElementWrapper
	@XmlElement(name = "button")
	private List<Button> toolbar;

	@XmlElementWrapper
	@XmlElement(name = "menu")
	private List<Menu> menubar;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@JsonIgnore
	public String getDefaultTitle() {
		return title;
	}

	public String getTitle() {
		return JPA.translate(title, title, this.getModel(), "view");
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

	private String widthPart(int which) {
		if (Strings.isNullOrEmpty(widthSpec)) {
			return null;
		}
		String[] parts = widthSpec.split(":");
		if (which >= parts.length) {
			return null;
		}
		String part = parts[which];
		if (part.matches("\\d+")) {
			part += "px";
		}
		return part;
	}

	public String getWidth() {
		return widthPart(0);
	}

	public String getMinWidth() {
		return widthPart(1);
	}

	public String getMaxWidth() {
		return widthPart(2);
	}

	public List<Button> getToolbar() {
		if(toolbar != null) {
			for (Button button : toolbar) {
				button.setModel(this.getModel());
			}
		}
		return toolbar;
	}

	public void setToolbar(List<Button> toolbar) {
		this.toolbar = toolbar;
	}

	public List<Menu> getMenubar() {
		if(menubar != null) {
			for (Menu menu : menubar) {
				menu.setModel(this.getModel());
			}
		}
		return menubar;
	}

	public void setMenubar(List<Menu> menubar) {
		this.menubar = menubar;
	}

	@XmlTransient
	public String getType() {
		try {
			return getClass().getAnnotation(JsonTypeName.class).value();
		} catch(Exception e){}
		return "unknown";
	}
}
