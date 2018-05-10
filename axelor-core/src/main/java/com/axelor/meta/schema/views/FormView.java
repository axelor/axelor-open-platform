/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.axelor.meta.loader.XMLViews;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("form")
public class FormView extends AbstractView {

	@XmlAttribute
	private Integer cols;

	@XmlAttribute
	private String colWidths;

	@XmlAttribute
	private String onLoad;

	@XmlAttribute
	private String onSave;

	@XmlAttribute
	private String onNew;

	@XmlAttribute
	private String readonlyIf;
	
	@XmlAttribute
	private String canNew;
	
	@XmlAttribute
	private String canEdit;
	
	@XmlAttribute
	private String canSave;
	
	@XmlAttribute
	private String canDelete;

	@XmlAttribute
	private String canArchive;

	@XmlAttribute
	private String canCopy;
	
	@XmlAttribute
	private String canAttach;

	@XmlElements({
		@XmlElement(name = "include", type = FormInclude.class),
		@XmlElement(name = "portlet", type = Portlet.class),
		@XmlElement(name = "group", type = Group.class),
        @XmlElement(name = "notebook", type = Notebook.class),
        @XmlElement(name = "field", type = Field.class),
        @XmlElement(name = "break", type = Break.class),
        @XmlElement(name = "spacer", type = Spacer.class),
        @XmlElement(name = "separator", type = Separator.class),
        @XmlElement(name = "label", type = Label.class),
        @XmlElement(name = "help", type = Help.class),
        @XmlElement(name = "button", type = Button.class),
        @XmlElement(name = "panel", type = Panel.class),
        @XmlElement(name = "panel-include", type = PanelInclude.class),
        @XmlElement(name = "panel-dashlet", type = Dashlet.class),
        @XmlElement(name = "panel-related", type = PanelRelated.class),
        @XmlElement(name = "panel-stack", type = PanelStack.class),
        @XmlElement(name = "panel-tabs", type = PanelTabs.class),
        @XmlElement(name = "panel-mail", type = PanelMail.class)
	})
	private List<AbstractWidget> items;

	public Integer getCols() {
		return cols;
	}

	public void setCols(Integer cols) {
		this.cols = cols;
	}

	public String getColWidths() {
		return colWidths;
	}

	public void setColWidths(String colWidths) {
		this.colWidths = colWidths;
	}

	public String getOnLoad() {
		return onLoad;
	}

	public void setOnLoad(String onLoad) {
		this.onLoad = onLoad;
	}

	public String getOnSave() {
		return onSave;
	}

	public void setOnSave(String onSave) {
		this.onSave = onSave;
	}

	public String getOnNew() {
		return onNew;
	}

	public void setOnNew(String onNew) {
		this.onNew = onNew;
	}

	public String getReadonlyIf() {
		return readonlyIf;
	}

	public void setReadonlyIf(String readonlyIf) {
		this.readonlyIf = readonlyIf;
	}
	
	public String getCanNew() {
		return canNew;
	}

	public void setCanNew(String canNew) {
		this.canNew = canNew;
	}

	public String getCanEdit() {
		return canEdit;
	}

	public void setCanEdit(String canEdit) {
		this.canEdit = canEdit;
	}

	public String getCanSave() {
		return canSave;
	}

	public void setCanSave(String canSave) {
		this.canSave = canSave;
	}

	public String getCanDelete() {
		return canDelete;
	}

	public void setCanDelete(String canDelete) {
		this.canDelete = canDelete;
	}

	public String getCanArchive() {
		return canArchive;
	}

	public void setCanArchive(String canArchive) {
		this.canArchive = canArchive;
	}

	public String getCanCopy() {
		return canCopy;
	}

	public void setCanCopy(String canCopy) {
		this.canCopy = canCopy;
	}

	public String getCanAttach() {
		return canAttach;
	}

	public void setCanAttach(String canAttach) {
		this.canAttach = canAttach;
	}

	@JsonIgnore
	public List<AbstractWidget> getItems() {
		if(items == null) {
			return items;
		}
		for(AbstractWidget item : items) {
			item.setModel(super.getModel());
			if (item instanceof FormInclude) {
				((FormInclude) item).setOwner(this);
			}
		}
		return items;
	}

	public void setItems(List<AbstractWidget> items) {
		this.items = items;
	}
	
	@XmlTransient
	@JsonProperty("items")
	public List<AbstractWidget> getItemsWithExtensions() {
		// check if this view is included in another view with same name
		// in that case, don't use extension view as owner will already use it.
		AbstractView owner = this.getOwner();
		while (owner != null) {
			if (Objects.equals(getName(), owner.getName())) {
				return getItems();
			}
			owner = owner.getOwner();
		}

		final List<AbstractWidget> items = getItems();
		final List<AbstractWidget> all = new ArrayList<>();
		if (items != null) {
			all.addAll(items);
		}
		final List<AbstractView> extensions = XMLViews.findExtensions(getName(), getModel(), "form", null);
		for (AbstractView extension : extensions) {
			if (extension instanceof FormView) {
				all.addAll(((FormView) extension).getItems());
			}
		}
		return all.isEmpty() ? null : all;
	}
}
