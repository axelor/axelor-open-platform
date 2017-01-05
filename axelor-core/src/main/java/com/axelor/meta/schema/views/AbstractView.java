/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.meta.schema.views;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.axelor.db.Query;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaView;
import com.fasterxml.jackson.annotation.JsonGetter;
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

	@XmlAttribute(name = "id")
	private String id;

	@XmlAttribute
	private String groups;

	@XmlAttribute
	private String helpLink;

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

	@JsonGetter("title")
	public String getLocalizedTitle() {
		return I18n.get(title);
	}

	@JsonIgnore
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getModel() {
		if(model != null)
			return model;

		MetaView view = Query.of(MetaView.class).filter("self.name = ?1", name).fetchOne();
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

	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	public String getGroups() {
		return groups;
	}

	public void setGroups(String groups) {
		this.groups = groups;
	}

	public String getHelpLink() {
		return helpLink;
	}

	public void setHelpLink(String helpLink) {
		this.helpLink = helpLink;
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
