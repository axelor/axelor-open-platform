/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("kanban")
public class KanbanView extends AbstractView {

	@XmlAttribute
	private String columnBy;

	@XmlAttribute
	private String colorBy;

	@XmlAttribute
	private String orderBy;

	@XmlAttribute
	private String onNew;

	@XmlAttribute
	private Integer limit;

	@XmlElement(name = "field", type = Field.class)
	private List<AbstractWidget> items;

	@XmlElement(name = "hilite", type = Hilite.class)
	private List<Hilite> hilites;

	@XmlElement
	private String template;

	public String getColumnBy() {
		return columnBy;
	}

	public void setColumnBy(String columnBy) {
		this.columnBy = columnBy;
	}

	public String getColorBy() {
		return colorBy;
	}

	public void setColorBy(String colorBy) {
		this.colorBy = colorBy;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}

	public String getOnNew() {
		return onNew;
	}

	public void setOnNew(String onNew) {
		this.onNew = onNew;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public List<AbstractWidget> getItems() {
		if (items == null) {
			return items;
		}
		for (AbstractWidget item : items) {
			item.setModel(getModel());
		}
		return items;
	}

	public void setItems(List<AbstractWidget> items) {
		this.items = items;
	}

	public List<Hilite> getHilites() {
		return hilites;
	}

	public void setHilites(List<Hilite> hilites) {
		this.hilites = hilites;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}
}