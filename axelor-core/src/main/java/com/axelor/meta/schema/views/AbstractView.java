/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.meta.schema.views;

import com.axelor.app.internal.AppFilter;
import com.axelor.auth.AuthUtils;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaView;
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
import jakarta.annotation.Nullable;
import jakarta.persistence.TypedQuery;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hibernate.jpa.QueryHints;

@XmlType
@JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "type")
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

  @XmlTransient @JsonProperty private Long customViewId;

  @XmlTransient @JsonProperty private Boolean customViewShared;

  @XmlAttribute private String name;

  @XmlAttribute private String title;

  @XmlAttribute private String css;

  @XmlAttribute private String model;

  @XmlAttribute private Boolean editable;

  @XmlAttribute private String groups;

  @XmlAttribute private String helpLink;

  @XmlAttribute private Boolean extension;

  @XmlAttribute(name = "x-json-model")
  private String jsonModel;

  @JsonIgnore
  @XmlAttribute(name = "width")
  private String widthSpec;

  @XmlTransient @JsonIgnore private transient AbstractView owner;

  @XmlTransient private String width;

  @XmlTransient private String minWidth;

  @XmlTransient private String maxWidth;

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

  public Long getCustomViewId() {
    return customViewId;
  }

  public void setCustomViewId(Long customViewId) {
    this.customViewId = customViewId;
  }

  public Boolean getCustomViewShared() {
    return customViewShared;
  }

  public void setCustomViewShared(Boolean customViewShared) {
    this.customViewShared = customViewShared;
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

  public String getWidthSpec() {
    computeWidthSpec();
    return widthSpec;
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

  public String getJsonModel() {
    return jsonModel;
  }

  public void setJsonModel(String jsonModel) {
    this.jsonModel = jsonModel;
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
    if (StringUtils.isBlank(widthSpec)) {
      return null;
    }
    String[] parts = widthSpec.split(":");
    return which >= parts.length ? null : parts[which];
  }

  private void computeWidthSpec() {
    String min = StringUtils.notBlank(minWidth) ? minWidth : null;
    String max = StringUtils.notBlank(maxWidth) ? maxWidth : null;
    String w = StringUtils.notBlank(width) ? width : minWidth;

    if (w == null && min == null && max == null) {
      return;
    }

    List<String> parts = new ArrayList<>();

    parts.add(w);

    if (min != null || max != null) parts.add(min);
    if (max != null) parts.add(max);

    widthSpec = parts.stream().map(x -> x == null ? "" : x).collect(Collectors.joining(":"));
  }

  private String ensureWidth(String width, int index) {
    if (StringUtils.notBlank(width)) {
      return width;
    }
    return widthPart(index);
  }

  public String getWidth() {
    return ensureWidth(width, 0);
  }

  public void setWidth(String width) {
    this.width = width;
  }

  public String getMinWidth() {
    return ensureWidth(minWidth, 1);
  }

  public void setMinWidth(String minWidth) {
    this.minWidth = minWidth;
  }

  public String getMaxWidth() {
    return ensureWidth(maxWidth, 2);
  }

  public void setMaxWidth(String maxWidth) {
    this.maxWidth = maxWidth;
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
  @JsonProperty(value = "helpOverride", access = JsonProperty.Access.READ_ONLY)
  @Nullable
  public Collection<Map<String, Object>> getHelpOverride() {
    if (AuthUtils.getUser() != null && Boolean.TRUE.equals(AuthUtils.getUser().getNoHelp())) {
      return null;
    }
    final Locale locale = AppFilter.getLocale();
    final String lang = locale.toLanguageTag();
    final String baseLang = locale.getLanguage();

    final TypedQuery<Object[]> query =
        JPA.em()
            .createQuery(
                """
                SELECT self.field, self.type, self.help, self.style \
                FROM MetaHelp self \
                WHERE self.model = :model AND (self.view = :view OR self.view IS NULL) AND self.language IN (:lang, :baseLang) \
                ORDER BY self.view ASC, self.language DESC""",
                Object[].class)
            .setParameter("lang", lang)
            .setParameter("baseLang", baseLang)
            .setParameter("model", getModel())
            .setParameter("view", getName())
            .setHint(QueryHints.HINT_CACHEABLE, true);

    final List<Object[]> found = query.getResultList();

    return found.isEmpty()
        ? null
        : found.stream()
            .map(
                a -> {
                  final Map<String, Object> map = new HashMap<>();
                  map.put("field", a[0]);
                  map.put("type", a[1]);
                  map.put("help", a[2]);
                  map.put("style", a[3]);
                  return map;
                })
            .collect(
                Collectors.toMap(
                    item -> (String) item.get("field"),
                    Function.identity(),
                    (existing, replacement) -> existing,
                    LinkedHashMap::new))
            .values();
  }
}
