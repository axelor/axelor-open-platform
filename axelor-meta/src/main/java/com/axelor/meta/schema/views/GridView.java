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
