/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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

import com.axelor.i18n.I18n;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@JsonTypeName("search-filters")
public class SearchFilters extends AbstractView implements ContainerView {

  @XmlElement(name = "field", type = Field.class)
  private List<AbstractWidget> items;

  @XmlElement(name = "filter")
  private List<SearchFilter> filters;

  @Override
  public List<AbstractWidget> getItems() {
    return items;
  }

  public void setItems(List<AbstractWidget> items) {
    this.items = items;
  }

  public List<SearchFilter> getFilters() {
    if (filters != null) {
      for (SearchFilter searchFilter : filters) {
        searchFilter.setModel(super.getModel());
      }
    }
    return filters;
  }

  public void setFilters(List<SearchFilter> filters) {
    this.filters = filters;
  }

  @XmlType
  @JsonInclude(Include.NON_NULL)
  public static class SearchFilter extends AbstractWidget {

    @XmlAttribute private String name;

    @XmlAttribute private String title;

    @XmlElement private String domain;

    @JsonIgnore
    @XmlElement(name = "context")
    private List<SearchContext> contexts;

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

    public void setTitle(String title) {
      this.title = title;
    }

    public String getDomain() {
      return domain;
    }

    public void setDomain(String domain) {
      this.domain = domain;
    }

    public Map<String, Object> getContext() {
      if (contexts == null || contexts.isEmpty()) {
        return null;
      }
      Map<String, Object> context = Maps.newHashMap();
      for (SearchContext ctx : contexts) {
        context.put(ctx.getName(), ctx.getValue());
      }
      return context;
    }

    public List<SearchContext> getContexts() {
      return contexts;
    }

    public void setContexts(List<SearchContext> contexts) {
      this.contexts = contexts;
    }
  }

  @XmlType
  public static class SearchContext {

    @XmlAttribute private String name;

    @XmlAttribute private String value;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }
}
