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

import com.axelor.db.JPA;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.PropertyType;
import com.axelor.meta.db.MetaSelect;
import com.axelor.meta.db.MetaSelectItem;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

@XmlType
@JsonTypeName("field")
public class Field extends SimpleWidget {

	@XmlAttribute
	private String placeholder;

	@XmlAttribute
	private String widget;

	@XmlAttribute
	private Boolean canSelect;

	@XmlAttribute
	private Boolean canNew;

	@XmlAttribute
	private Boolean canRemove;

	@XmlAttribute
	private Boolean canView;

	@XmlAttribute
	private String onChange;

	@XmlAttribute
	private String onSelect;

	@XmlAttribute
	private String domain;

	@XmlAttribute
	private Boolean required;

	@XmlAttribute
	private String requiredIf;

	@XmlAttribute
	private String validIf;

	@XmlAttribute(name = "min")
	private String minSize;

	@XmlAttribute(name = "max")
	private String maxSize;

	@XmlAttribute
	private String pattern;

	@XmlAttribute
	private String fgColor;

	@XmlAttribute
	private String bgColor;

	@XmlAttribute
	private String selection;

	@XmlAttribute(name = "selection-in")
	private String selectionIn;

	@XmlAttribute
	private String aggregate;

	@XmlAttribute(name = "edit-window")
	private String editWindow;

	@XmlAttribute(name = "form-view")
	private String formView;

	@XmlAttribute(name = "grid-view")
	private String gridView;

	@XmlAttribute(name = "summary-view")
	private String summaryView;

	@XmlElement(name = "hilite")
	private List<Hilite> hilites;

	@XmlElements({
		@XmlElement(name = "form", type = FormView.class),
		@XmlElement(name = "grid", type = GridView.class)
	})
	private List<AbstractView> views;

	@XmlAttribute
	@JsonIgnore
	private Boolean export;

	@XmlAttribute
	@JsonIgnore
	private Boolean documentation;

	@Override
	public String getTitle() {
		if(!Strings.isNullOrEmpty(super.getDefaultTitle())) {
			return JPA.translate(super.getDefaultTitle(), super.getDefaultTitle(), this.getModel(), "field");
		}
		return JPA.translate(this.getName(), super.getDefaultTitle(), this.getModel(), "field");
	}

	@Override
	public String getHelp() {
		if(!Strings.isNullOrEmpty(super.getDefaultHelp())) {
			return JPA.translate(super.getDefaultHelp(), super.getDefaultHelp(), this.getModel(), "help");
		}
		return JPA.translate(this.getName(), super.getDefaultHelp(), this.getModel(), "help");
	}

	public String getDefaultPlaceholder() {
		return placeholder;
	}

	public String getPlaceholder() {
		if (!Strings.isNullOrEmpty(placeholder)) {
			return JPA.translate(placeholder, placeholder, this.getModel(), "placeholder");
		}
		return placeholder;
	}

	public void setPlaceholder(String placeholder) {
		this.placeholder = placeholder;
	}

	public String getWidget() {
		return widget;
	}

	public void setWidget(String widget) {
		this.widget = widget;
	}

	public Boolean getCanSelect() {
		return canSelect;
	}

	public void setCanSelect(Boolean canSelect) {
		this.canSelect = canSelect;
	}

	public Boolean getCanNew() {
		return canNew;
	}

	public void setCanNew(Boolean canNew) {
		this.canNew = canNew;
	}

	public Boolean getCanRemove() {
		return canRemove;
	}

	public void setCanRemove(Boolean canRemove) {
		this.canRemove = canRemove;
	}

	public Boolean getCanView() {
		return canView;
	}

	public void setCanView(Boolean canView) {
		this.canView = canView;
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

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
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

	public String getSelection() {
		return selection;
	}

	public String getSelectionIn() {
		return selectionIn;
	}

	public void setSelectionIn(String selectionIn) {
		this.selectionIn = selectionIn;
	}

	public List<Object> getSelectionList() {
		String selection = getSelection();
		if (selection == null || "".equals(selection.trim()))
			return null;
		final MetaSelect select = MetaSelect.findByName(selection);
		final List<Object> all = Lists.newArrayList();
		if (select == null || select.getItems() == null) {
			return all;
		}
		List<MetaSelectItem> items = MetaSelectItem.all().filter("self.select.id = ?", select.getId()).order("order").fetch();
		if (items == null || items.isEmpty()) {
			return null;
		}
		for(MetaSelectItem item : items) {
			all.add(ImmutableMap.of("value", item.getValue(), "title", JPA.translate(item.getTitle(), item.getTitle(), null, "select")));
		}
		return all;
	}

	public void setSelection(String selection) {
		this.selection = selection;
	}

	public String getAggregate() {
		return aggregate;
	}

	public void setAggregate(String aggregate) {
		this.aggregate = aggregate;
	}

	public List<Hilite> getHilites() {
		return hilites;
	}

	public void setHilites(List<Hilite> hilites) {
		this.hilites = hilites;
	}

	public List<AbstractView> getViews() {
		if(views != null && this.getTarget() != null) {
			for (AbstractView abstractView : views) {
				abstractView.setModel(this.getTarget());
			}
		}
		return views;
	}

	public void setViews(List<AbstractView> views) {
		this.views = views;
	}

	public String getMinSize() {
		return minSize;
	}

	public void setMinSize(String minSize) {
		this.minSize = minSize;
	}

	public String getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(String maxSize) {
		this.maxSize = maxSize;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public String getFgColor() {
		return fgColor;
	}

	public void setFgColor(String fgColor) {
		this.fgColor = fgColor;
	}

	public String getBgColor() {
		return bgColor;
	}

	public void setBgColor(String bgColor) {
		this.bgColor = bgColor;
	}

	public String getEditWindow() {
		return editWindow;
	}

	public void setEditWindow(String editWindow) {
		this.editWindow = editWindow;
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

	public String getSummaryView() {
		return summaryView;
	}

	public void setSummaryView(String summaryView) {
		this.summaryView = summaryView;
	}

	public Boolean getExport() {
		return export == null ? true : export;
	}

	public void setExport(Boolean export) {
		this.export = export;
	}

	public Boolean getDocumentation() {
		if(documentation != null) {
			return documentation;
		}

		Mapper mapper = null;
		try {
			mapper = Mapper.of(Class.forName(this.getModel()));
			PropertyType type = mapper.getProperty(getName()).getType();
			if(type == PropertyType.ONE_TO_ONE || type == PropertyType.MANY_TO_ONE || type == PropertyType.MANY_TO_MANY) {
				return documentation == null ? false : documentation;
			}
			else if(type == PropertyType.ONE_TO_MANY) {
				return documentation == null ? true : documentation;
			}
		} catch (Exception e) {
		}
		return false;
	}

	public void setDocumentation(Boolean documentation) {
		this.documentation = documentation;
	}

	public String getTarget() {
		Mapper mapper = null;
		try {
			mapper = Mapper.of(Class.forName(this.getModel()));
			return mapper.getProperty(getName()).getTarget().getName();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (NullPointerException e) {
		}
		return null;
	}
}
