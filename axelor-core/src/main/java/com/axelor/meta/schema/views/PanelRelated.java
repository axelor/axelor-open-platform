/*
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.axelor.common.StringUtils;
import com.axelor.db.mapper.Mapper;
import com.axelor.meta.MetaStore;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonTypeName;

@XmlType
@JsonTypeName("panel-related")
public class PanelRelated extends AbstractPanel {

	@XmlAttribute(name = "field")
	private String name;

	@XmlAttribute(name = "type")
	private String serverType;

	@XmlAttribute(name = "form-view")
	private String formView;
	
	@XmlAttribute(name = "grid-view")
	private String gridView;

	@XmlAttribute(name = "x-search-limit")
	private Integer searchLimit;

	@XmlAttribute(name = "x-row-height")
	private Integer rowHeight;

	@XmlAttribute
	private Boolean editable;

	@XmlAttribute
	private Boolean required;

	@XmlAttribute
	private String requiredIf;

	@XmlAttribute
	private String validIf;

	@XmlAttribute
	private String orderBy;

	@XmlAttribute
	private String groupBy;

	@XmlAttribute
	private String domain;
	
	@XmlAttribute
	private String target;

	@XmlAttribute(name = "target-name")
	private String targetName;

	@XmlAttribute
	private String onNew;

	@XmlAttribute
	private String onChange;
	
	@XmlAttribute
	private String onSelect;
	
	@XmlAttribute
	private String canSelect;

	@XmlAttribute
	private String canNew;

	@XmlAttribute
	private String canView;

	@XmlAttribute
	private String canEdit;

	@XmlAttribute
	private String canRemove;

	@XmlAttribute
	private Boolean canMove;

	@XmlAttribute(name = "edit-window")
	private String editWindow;

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

	public String getServerType() {
		if (serverType == null) {
			try {
				Mapper mapper = Mapper.of(Class.forName(this.getModel()));
				serverType = mapper.getProperty(getName()).getType().name();
			} catch (Exception e) {
			}
		}
		return serverType;
	}

	public void setServerType(String serverType) {
		this.serverType = serverType;
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

	public Integer getSearchLimit() {
		return searchLimit;
	}

	public void setSearchLimit(Integer searchLimit) {
		this.searchLimit = searchLimit;
	}

	public Integer getRowHeight() {
		return rowHeight;
	}
	
	public void setRowHeight(Integer rowHeight) {
		this.rowHeight = rowHeight;
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

	public Boolean getEditable() {
		return editable;
	}
	
	public void setEditable(Boolean editable) {
		this.editable = editable;
	}

	public Boolean getRequired() {
		return required;
	}

	public void setRequired(Boolean required) {
		this.required = required;
	}

	public String getRequiredIf() {
		return requiredIf;
	}

	public void setRequiredIf(String requiredIf) {
		this.requiredIf = requiredIf;
	}

	public String getValidIf() {
		return validIf;
	}

	public void setValidIf(String validIf) {
		this.validIf = validIf;
	}

	public String getDomain() {
		return domain;
	}
	
	public void setDomain(String domain) {
		this.domain = domain;
	}
	
	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getTargetName() {
		return targetName;
	}

	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	public String getOnNew() {
		return onNew;
	}

	public void setOnNew(String onNew) {
		this.onNew = onNew;
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

	public String getCanNew() {
		return canNew;
	}

	public void setCanNew(String canNew) {
		this.canNew = canNew;
	}

	public String getCanView() {
		return canView;
	}

	public void setCanView(String canView) {
		this.canView = canView;
	}

	public String getCanEdit() {
		return canEdit;
	}

	public void setCanEdit(String canEdit) {
		this.canEdit = canEdit;
	}

	public String getCanRemove() {
		return canRemove;
	}

	public void setCanRemove(String canRemove) {
		this.canRemove = canRemove;
	}

	public String getCanSelect() {
		return canSelect;
	}

	public void setCanSelect(String canSelect) {
		this.canSelect = canSelect;
	}

	public Boolean getCanMove() {
		return canMove;
	}

	public void setCanMove(Boolean canMove) {
		this.canMove = canMove;
	}

	public String getEditWindow() {
		return editWindow;
	}

	public void setEditWindow(String editWindow) {
		this.editWindow = editWindow;
	}

	public List<AbstractWidget> getItems() {
		return process(items);
	}

	public void setItems(List<AbstractWidget> items) {
		this.items = items;
	}

	public Map<String, Object> getPerms() {
		try {
			return MetaStore.getPermissions(Class.forName(getTarget()));
		} catch (Exception e) {}
		return null;
	}
	
	@JsonGetter("fields")
	public List<Object> getTargetFields() {
		if (getItems() == null || getItems().isEmpty()) {
			return null;
		}

		Class<?> target;
		try {
			target = Class.forName(getTarget());
		} catch (Exception e) {
			try {
				target = Mapper.of(Class.forName(getModel())).getProperty(getName()).getTarget();
			} catch (Exception ex) {
				return null;
			}
		}
		if (target == null) {
			return null;
		}
		
		List<Object> targetFields = new ArrayList<>();
		List<String> names = getItems()
				.stream()
				.filter(x -> x instanceof SimpleWidget)
				.map(x -> ((SimpleWidget) x).getName())
				.filter(n -> !StringUtils.isBlank(n))
				.collect(Collectors.toList());

		final Map<String, Object> fields = MetaStore.findFields(target, names);
		targetFields.addAll((Collection<?>)fields.get("fields"));

		return targetFields;
	}
}
