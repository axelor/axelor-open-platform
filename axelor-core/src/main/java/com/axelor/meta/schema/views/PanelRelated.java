/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("panel-related")
public class PanelRelated extends AbstractPanel {

	@XmlAttribute(name = "field")
	private String name;

	@XmlAttribute(name = "form-view")
	private String formView;
	
	@XmlAttribute(name = "grid-view")
	private String gridView;
	
	@XmlAttribute
	private Boolean editable;

	@XmlAttribute
	private String orderBy;

	@XmlAttribute
	private String domain;
	
	@XmlAttribute
	private String onChange;
	
	@XmlAttribute
	private String onSelect;

	@XmlAttribute
	private Boolean canNew;

	@XmlAttribute
	private Boolean canView;

	@XmlAttribute
	private Boolean canRemove;

	@XmlElements({
		@XmlElement(name = "field", type = PanelField.class),
		@XmlElement(name = "button", type = Button.class),
	})
	private List<AbstractWidget> items;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFormView() {
		return formView;
	}
	
	public void setFormView(String formView) {
		this.formView = formView;
	}
	
	public String getGridView() {
		return gridView;
	}
	
	public void setGridView(String gridView) {
		this.gridView = gridView;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}

	public Boolean getEditable() {
		return editable;
	}
	
	public void setEditable(Boolean editable) {
		this.editable = editable;
	}
	
	public String getDomain() {
		return domain;
	}
	
	public void setDomain(String domain) {
		this.domain = domain;
	}
	
	public String getOnChange() {
		return onChange;
	}
	
	public void setOnChange(String onChange) {
		this.onChange = onChange;
	}
	
	public String getOnSelect() {
		return onSelect;
	}
	
	public void setOnSelect(String onSelect) {
		this.onSelect = onSelect;
	}
	
	public Boolean getCanNew() {
		return canNew;
	}
	
	public void setCanNew(Boolean canNew) {
		this.canNew = canNew;
	}
	
	public Boolean getCanView() {
		return canView;
	}
	
	public void setCanView(Boolean canView) {
		this.canView = canView;
	}

	public Boolean getCanRemove() {
		return canRemove;
	}

	public void setCanRemove(Boolean canRemove) {
		this.canRemove = canRemove;
	}

	public List<AbstractWidget> getItems() {
		return process(items);
	}

	public void setItems(List<AbstractWidget> items) {
		this.items = items;
	}
}
