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
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlType
@JsonTypeName("chart")
public class ChartView extends AbstractView {

  @XmlAttribute private Boolean stacked;

  @XmlAttribute private String onInit;

  @XmlElement(name = "field", type = ChartSearchField.class)
  @XmlElementWrapper(name = "search-fields")
  private List<ChartSearchField> searchFields;

  @JsonIgnore
  @XmlElement(name = "dataset")
  private DataSet dataSet;

  @XmlElement private ChartCategory category;

  @XmlElement private List<ChartSeries> series;

  @XmlElement private List<ChartConfig> config;

  @XmlElement(name = "action", type = ChartAction.class)
  @XmlElementWrapper(name = "actions")
  private List<ChartAction> actions;

  public Boolean getStacked() {
    return stacked;
  }

  public void setStacked(Boolean stacked) {
    this.stacked = stacked;
  }

  public String getOnInit() {
    return onInit;
  }

  public void setOnInit(String onInit) {
    this.onInit = onInit;
  }

  public List<ChartSearchField> getSearchFields() {
    return searchFields;
  }

  public void setSearchFields(List<ChartSearchField> searchFields) {
    this.searchFields = searchFields;
  }

  public DataSet getDataSet() {
    return dataSet;
  }

  public void setDataSet(DataSet dataSet) {
    this.dataSet = dataSet;
  }

  public ChartCategory getCategory() {
    return category;
  }

  public void setCategory(ChartCategory category) {
    this.category = category;
  }

  public List<ChartSeries> getSeries() {
    return series;
  }

  public void setSeries(List<ChartSeries> series) {
    this.series = series;
  }

  public List<ChartConfig> getConfig() {
    return config;
  }

  public void setConfig(List<ChartConfig> config) {
    this.config = config;
  }

  public List<ChartAction> getActions() {
    return actions;
  }

  public void setActions(List<ChartAction> actions) {
    this.actions = actions;
  }

  @XmlType
  @JsonTypeName("category")
  public static class ChartCategory {

    @XmlAttribute private String key;

    @XmlAttribute private String type;

    @XmlAttribute private String title;

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
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
  }

  @XmlType
  @JsonTypeName("series")
  public static class ChartSeries {

    @XmlAttribute private String key;

    @XmlAttribute private String groupBy;

    @XmlAttribute private String type;

    @XmlAttribute private String side;

    @XmlAttribute private String title;

    @XmlAttribute private String aggregate;

    @XmlAttribute private Integer scale;

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getGroupBy() {
      return groupBy;
    }

    public void setGroupBy(String groupBy) {
      this.groupBy = groupBy;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getSide() {
      return side;
    }

    public void setSide(String side) {
      this.side = side;
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

    public String getAggregate() {
      return aggregate;
    }

    public void setAggregate(String aggregate) {
      this.aggregate = aggregate;
    }

    public Integer getScale() {
      return scale;
    }

    public void setScale(Integer scale) {
      this.scale = scale;
    }
  }

  @XmlType
  public static class ChartConfig {

    @XmlAttribute private String name;

    @XmlAttribute private String value;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  @XmlType
  public static class ChartAction {

    @XmlAttribute private String name;

    @XmlAttribute private String title;

    @XmlAttribute private String action;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    @JsonIgnore
    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getAction() {
      return action;
    }

    public void setAction(String action) {
      this.action = action;
    }

    @JsonGetter("title")
    public String getLocalizedTitle() {
      return I18n.get(title);
    }
  }
}
