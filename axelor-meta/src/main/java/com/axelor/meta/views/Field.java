package com.axelor.meta.views;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.axelor.db.JPA;
import com.axelor.db.mapper.Mapper;
import com.axelor.meta.db.MetaSelect;
import com.axelor.meta.db.MetaSelectItem;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

@XmlType
@JsonTypeName("field")
public class Field extends SimpleWidget {

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
	
	@XmlAttribute
	private Integer width;
	
	@XmlAttribute(name = "min")
	private String minSize;
	
	@XmlAttribute(name = "max")
	private String maxSize;
	
	@XmlAttribute
	private String fgColor;
	
	@XmlAttribute
	private String bgColor;
	
	@XmlAttribute
	private String selection;
	
	@XmlAttribute(name = "edit-window")
	private String editWindow;
	
	@XmlAttribute(name = "form-view")
	private String formView;
	
	@XmlAttribute(name = "grid-view")
	private String gridView;
	
	@XmlAttribute(name = "summary-view")
	private String summaryView;
	
	private Hilite hilite;
	
	@XmlElements({
		@XmlElement(name = "form", type = FormView.class),
		@XmlElement(name = "grid", type = GridView.class)
	})
	private List<AbstractView> views;
	
	@Override
	public String getTitle() {
		return JPA.translate(this.getName(), super.getDefaultTitle(), this.getModel(), "title");
	}
	
	@Override
	public String getHelp() {
		return JPA.translate(this.getName(), super.getDefaultHelp(), this.getModel(), "help");
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

	public Integer getWidth() {
		return width;
	}
	
	public void setWidth(Integer width) {
		this.width = width;
	}

	@JsonIgnore
	public String getSelection() {
		return selection;
	}

	@JsonProperty("selection")
	public List<Object> getSelectionList() {
		if (selection == null || "".equals(selection.trim()))
			return null;
		final MetaSelect select = MetaSelect.all().filter("self.name = ?", selection).fetchOne();
		final List<Object> all = Lists.newArrayList();
		if (select == null || select.getItems() == null) {
			return all;
		}
		for(MetaSelectItem item : select.getItems()) {
			all.add(ImmutableMap.of("value", item.getValue(), "title", JPA.translate(item.getTitle())));
		}
		return all;
	}
	
	public void setSelection(String selection) {
		this.selection = selection;
	}
	
	public Hilite getHilite() {
		return hilite;
	}

	public void setHilite(Hilite hilite) {
		this.hilite = hilite;
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
	
	private String getTarget() {
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
