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

import com.axelor.common.StringUtils;
import com.axelor.db.mapper.Mapper;
import com.axelor.i18n.I18n;
import com.axelor.meta.MetaStore;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlType
@JsonTypeName("field")
public class Field extends SimpleWidget {

  @XmlAttribute(name = "type")
  private String serverType;

  @XmlAttribute private String placeholder;

  @XmlAttribute private String widget;

  @XmlAttribute private Boolean canSuggest;

  @XmlAttribute private String canSelect;

  @XmlAttribute private String canNew;

  @XmlAttribute private String canView;

  @XmlAttribute private String canEdit;

  @XmlAttribute private String canRemove;

  @XmlAttribute private String onChange;

  @XmlAttribute private String onSelect;

  @XmlAttribute private String target;

  @XmlAttribute(name = "target-name")
  private String targetName;

  @XmlAttribute private String domain;

  @XmlAttribute private Boolean required;

  @XmlAttribute private String requiredIf;

  @XmlAttribute private String validIf;

  @XmlAttribute(name = "min")
  private String minSize;

  @XmlAttribute(name = "max")
  private String maxSize;

  @XmlAttribute private String pattern;

  @XmlAttribute private String fgColor;

  @XmlAttribute private String bgColor;

  @XmlAttribute private String selection;

  @XmlAttribute(name = "selection-in")
  private String selectionIn;

  @XmlAttribute private String aggregate;

  @XmlAttribute private Boolean massUpdate;

  @XmlAttribute(name = "edit-window")
  private String editWindow;

  @XmlAttribute(name = "form-view")
  private String formView;

  @XmlAttribute(name = "grid-view")
  private String gridView;

  @XmlAttribute(name = "summary-view")
  private String summaryView;

  @XmlAttribute(name = "x-bind")
  private String bind;

  @XmlAttribute(name = "x-enum-type")
  private String enumType;

  @XmlAttribute(name = "x-related")
  private String related;

  @XmlAttribute(name = "x-create")
  private String create;

  @XmlAttribute(name = "x-can-reload")
  private Boolean canReload;

  @XmlAttribute(name = "x-call-onSave")
  private Boolean callOnSave;

  @XmlAttribute(name = "x-icon")
  private String icon;

  @XmlAttribute(name = "x-icon-hover")
  private String iconHover;

  @XmlAttribute(name = "x-icon-active")
  private String iconActive;

  @XmlAttribute(name = "x-exclusive")
  private Boolean exclusive;

  @XmlAttribute(name = "x-show-icons")
  private String showIcons;

  @XmlAttribute(name = "x-direction")
  private String direction;

  @XmlAttribute(name = "x-code-syntax")
  private String codeSyntax;

  @XmlAttribute(name = "x-code-theme")
  private String codeTheme;

  @XmlAttribute(name = "x-lite")
  private Boolean lite;

  @XmlAttribute(name = "x-labels")
  private Boolean labels;

  @XmlAttribute(name = "x-order")
  private String orderBy;

  @XmlAttribute(name = "x-limit")
  private Integer limit;

  @XmlAttribute(name = "x-search-limit")
  private Integer searchLimit;

  @XmlAttribute(name = "x-json-model")
  private String jsonModel;

  @XmlElement(name = "hilite")
  private List<Hilite> hilites;

  @XmlElements({
    @XmlElement(name = "form", type = FormView.class),
    @XmlElement(name = "grid", type = GridView.class)
  })
  private List<AbstractView> views;

  public String getServerType() {
    return serverType;
  }

  public void setServerType(String serverType) {
    this.serverType = serverType;
  }

  @JsonGetter("placeholder")
  public String getLocalizedPlaceholder() {
    return I18n.get(placeholder);
  }

  @JsonIgnore
  public String getPlaceholder() {
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

  public Boolean getCanSuggest() {
    return canSuggest;
  }

  public void setCanSuggest(Boolean canSuggest) {
    this.canSuggest = canSuggest;
  }

  public String getCanSelect() {
    return canSelect;
  }

  public void setCanSelect(String canSelect) {
    this.canSelect = canSelect;
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

  public String getTarget() {
    if (target == null) {
      return getTargetModel();
    }
    return target;
  }

  public String getTargetName() {
    if (targetName != null) return targetName;
    String targetModel = getTarget();
    if (targetModel == null) return null;
    try {
      return Mapper.of(Class.forName(targetModel)).getNameField().getName();
    } catch (Exception e) {
    }
    return null;
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

  public List<?> getSelectionList() {
    final String typeName =
        StringUtils.isBlank(enumType) && "enum".equals(serverType) ? target : enumType;
    if (StringUtils.notBlank(typeName)) {
      try {
        return MetaStore.getSelectionList(Class.forName(typeName));
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("No such enum type found: " + typeName, e);
      }
    }
    return MetaStore.getSelectionList(getSelection());
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

  public Boolean getMassUpdate() {
    return massUpdate;
  }

  public void setMassUpdate(Boolean massUpdate) {
    this.massUpdate = massUpdate;
  }

  public String getBind() {
    return bind;
  }

  public String getEnumType() {
    return enumType;
  }

  public String getRelated() {
    if (StringUtils.isBlank(related)
        && StringUtils.notBlank(widget)
        && widget.matches("RefSelect|ref-select")) {
      return getName() + "Id";
    }
    return related;
  }

  public String getCreate() {
    return create;
  }

  public Boolean getCanReload() {
    return canReload;
  }

  public void setCanReload(Boolean canReload) {
    this.canReload = canReload;
  }

  public Boolean getCallOnSave() {
    return callOnSave;
  }

  public void setCallOnSave(Boolean callOnSave) {
    this.callOnSave = callOnSave;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getIconHover() {
    return iconHover;
  }

  public void setIconHover(String iconHover) {
    this.iconHover = iconHover;
  }

  public String getIconActive() {
    return iconActive;
  }

  public void setIconActive(String iconActive) {
    this.iconActive = iconActive;
  }

  public Boolean getExclusive() {
    return exclusive;
  }

  public void setExclusive(Boolean exclusive) {
    this.exclusive = exclusive;
  }

  public String getShowIcons() {
    return showIcons;
  }

  public void setShowIcons(String showIcons) {
    this.showIcons = showIcons;
  }

  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }

  public String getCodeSyntax() {
    return codeSyntax;
  }

  public void setCodeSyntax(String codeSyntax) {
    this.codeSyntax = codeSyntax;
  }

  public String getCodeTheme() {
    return codeTheme;
  }

  public void setCodeTheme(String codeTheme) {
    this.codeTheme = codeTheme;
  }

  public Boolean getLite() {
    return lite;
  }

  public void setLite(Boolean lite) {
    this.lite = lite;
  }

  public Boolean getLabels() {
    return labels;
  }

  public void setLabels(Boolean labels) {
    this.labels = labels;
  }

  public String getOrderBy() {
    return orderBy;
  }

  public void setOrderBy(String orderBy) {
    this.orderBy = orderBy;
  }

  public Integer getLimit() {
    return limit;
  }

  public void setLimit(Integer limit) {
    this.limit = limit;
  }

  public Integer getSearchLimit() {
    return searchLimit;
  }

  public void setSearchLimit(Integer searchLimit) {
    this.searchLimit = searchLimit;
  }

  public String getJsonModel() {
    return jsonModel;
  }

  public void setJsonModel(String jsonModel) {
    this.jsonModel = jsonModel;
  }

  public List<Hilite> getHilites() {
    return hilites;
  }

  public void setHilites(List<Hilite> hilites) {
    this.hilites = hilites;
  }

  public List<AbstractView> getViews() {
    if (views != null && this.getTarget() != null) {
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

  private String getTargetModel() {
    Mapper mapper = null;
    try {
      mapper = Mapper.of(Class.forName(this.getModel()));
      return mapper.getProperty(getName()).getTarget().getName();
    } catch (ClassNotFoundException e) {
      return null;
    } catch (NullPointerException e) {
    }
    return null;
  }

  @XmlTransient
  @JsonProperty
  public Collection<?> getJsonFields() {
    final Map<String, Object> fields =
        StringUtils.isBlank(jsonModel)
            ? MetaStore.findJsonFields(getModel(), getName())
            : MetaStore.findJsonFields(jsonModel);
    return fields == null ? null : fields.values();
  }
}
