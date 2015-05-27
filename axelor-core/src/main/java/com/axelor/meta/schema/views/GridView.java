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
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("grid")
public class GridView extends AbstractView {

	@XmlAttribute
    private Boolean expandable;

	@XmlAttribute
	private String orderBy;

	@XmlAttribute
	private String groupBy;

	@XmlAttribute
	private Boolean customSearch;

	@XmlAttribute
	private String freeSearch;

	@XmlAttribute
	private String onNew;

	@XmlAttribute
	private Boolean canNew;
	
	@XmlAttribute
	private Boolean canEdit;
	
	@XmlAttribute
	private Boolean canSave;
	
	@XmlAttribute
	private Boolean canDelete;
	
	@XmlAttribute(name = "edit-icon")
	private Boolean editIcon = Boolean.TRUE;

	@XmlElement(name = "hilite")
	private List<Hilite> hilites;

	@XmlElements({
		@XmlElement(name="field", type=Field.class),
		@XmlElement(name="button", type=Button.class)
	})
	private List<AbstractWidget> items;

	public Boolean getExpandable() {
		return expandable;
	}

	public void setExpandable(Boolean expandable) {
		this.expandable = expandable;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}

	public String getGroupBy() {
		return groupBy;
	}

	public void setGroupBy(String groupBy) {
		this.groupBy = groupBy;
	}

	public Boolean getCustomSearch() {
		return customSearch;
	}

	public void setCustomSearch(Boolean customSearch) {
		this.customSearch = customSearch;
	}

	public String getFreeSearch() {
		return freeSearch;
	}

	public void setFreeSearch(String freeSearch) {
		this.freeSearch = freeSearch;
	}

	public String getOnNew() {
		return onNew;
	}

	public void setOnNew(String onNew) {
		this.onNew = onNew;
	}

	public Boolean getCanNew() {
		return canNew;
	}

	public void setCanNew(Boolean canNew) {
		this.canNew = canNew;
	}

	public Boolean getCanEdit() {
		return canEdit;
	}

	public void setCanEdit(Boolean canEdit) {
		this.canEdit = canEdit;
	}

	public Boolean getCanSave() {
		return canSave;
	}

	public void setCanSave(Boolean canSave) {
		this.canSave = canSave;
	}

	public Boolean getCanDelete() {
		return canDelete;
	}

	public void setCanDelete(Boolean canDelete) {
		this.canDelete = canDelete;
	}

	public Boolean getEditIcon() {
		return editIcon;
	}

	public void setEditIcon(Boolean editIcon) {
		this.editIcon = editIcon;
	}

	public List<Hilite> getHilites() {
		return hilites;
	}

	public void setHilites(List<Hilite> hilites) {
		this.hilites = hilites;
	}

	public List<AbstractWidget> getItems() {
		if(items != null) {
			for (AbstractWidget field : items) {
				field.setModel(super.getModel());
			}
		}
		return items;
	}

	public void setItems(List<AbstractWidget> items) {
		this.items = items;
	}
}
