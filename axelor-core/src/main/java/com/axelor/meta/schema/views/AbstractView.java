/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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

import com.axelor.app.internal.AppFilter;
import com.axelor.auth.AuthUtils;
import com.axelor.db.Query;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.db.repo.MetaHelpRepository;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Strings;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlType
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonInclude(Include.NON_NULL)
@JsonSubTypes({
  @Type(GridView.class),
  @Type(FormView.class),
  @Type(TreeView.class),
  @Type(ChartView.class),
  @Type(CalendarView.class),
  @Type(GanttView.class),
  @Type(CardsView.class),
  @Type(KanbanView.class),
  @Type(CustomView.class),
  @Type(Dashboard.class),
  @Type(Search.class),
  @Type(SearchFilters.class)
})
public abstract class AbstractView {

  @XmlAttribute(name = "id")
  private String xmlId;

  @XmlTransient @JsonProperty private Long viewId;

  @XmlTransient @JsonProperty private Long modelId;

  @XmlAttribute private String name;

  @XmlAttribute private String title;

  @XmlAttribute private String css;

  @XmlAttribute private String model;

  @XmlAttribute private Boolean editable;

  @XmlAttribute private String groups;

  @XmlAttribute private String helpLink;

  @XmlAttribute private Boolean extension;

  @JsonIgnore
  @XmlAttribute(name = "width")
  private String widthSpec;

  @XmlElementWrapper
  @XmlElement(name = "button")
  private List<Button> toolbar;

  @XmlElementWrapper
  @XmlElement(name = "menu")
  private List<Menu> menubar;

  @XmlTransient @JsonIgnore private transient AbstractView owner;

  public String getXmlId() {
    return xmlId;
  }

  public void setXmlId(String xmlId) {
    this.xmlId = xmlId;
  }

  public Long getViewId() {
    return viewId;
  }

  public void setViewId(Long viewId) {
    this.viewId = viewId;
  }

  public Long getModelId() {
    return modelId;
  }

  public void setModelId(Long modelId) {
    this.modelId = modelId;
  }

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

  @JsonSetter
  public void setTitle(String title) {
    this.title = title;
  }

  public String getCss() {
    return css;
  }

  public void setCss(String css) {
    this.css = css;
  }

  public void setWidthSpec(String widthSpec) {
    this.widthSpec = widthSpec;
  }

  public String getModel() {
    if (model != null) return model;

    MetaView view = Query.of(MetaView.class).filter("self.name = ?1", name).fetchOne();
    if (view != null && view.getModel() != null) {
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

  public Boolean getExtension() {
    return extension;
  }

  public void setExtension(Boolean extension) {
    this.extension = extension;
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
    if (toolbar != null) {
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
    if (menubar != null) {
      for (Menu menu : menubar) {
        menu.setModel(this.getModel());
      }
    }
    return menubar;
  }

  public void setMenubar(List<Menu> menubar) {
    this.menubar = menubar;
  }

  public AbstractView getOwner() {
    return owner;
  }

  public void setOwner(AbstractView owner) {
    this.owner = owner;
  }

  @XmlTransient
  public String getType() {
    try {
      return getClass().getAnnotation(JsonTypeName.class).value();
    } catch (Exception e) {
    }
    return "unknown";
  }

  @XmlTransient
  @JsonProperty("helpOverride")
  public List<?> getHelpOverride() {
    if (AuthUtils.getUser() != null && AuthUtils.getUser().getNoHelp() == Boolean.TRUE) {
      return null;
    }
    final MetaHelpRepository repo = Beans.get(MetaHelpRepository.class);
    final String lang = AppFilter.getLocale() == null ? "en" : AppFilter.getLocale().getLanguage();
    List<?> found =
        repo.all()
            .filter(
                "self.model = :model AND self.language = :lang and (self.view = :view OR self.view IS NULL)")
            .bind("model", getModel())
            .bind("lang", lang)
            .bind("view", getName())
            .order("-view")
            .select("field", "type", "help", "style")
            .fetch(-1, 0);
    return found.isEmpty() ? null : found;
  }
}
