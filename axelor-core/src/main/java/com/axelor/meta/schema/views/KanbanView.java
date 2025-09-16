/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import com.axelor.common.StringUtils;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.MetaStore;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@XmlType
@JsonTypeName("kanban")
public class KanbanView extends CardsView {

  @XmlAttribute private String columnBy;

  @XmlAttribute private String sequenceBy;

  @XmlAttribute private Boolean draggable = Boolean.TRUE;

  @XmlAttribute private String onNew;

  @XmlAttribute private String onMove;

  @XmlAttribute private Integer limit;

  @JsonIgnore
  @XmlAttribute(name = "x-limit-columns")
  private Integer limitColumns;

  @XmlAttribute(name = "x-collapse-columns")
  private String collapseColumns;

  @Override
  public Set<String> getExtraNames() {
    return Stream.of(getColumnBy(), getSequenceBy())
        .filter(StringUtils::notBlank)
        .collect(Collectors.toSet());
  }

  public String getColumnBy() {
    return columnBy;
  }

  public void setColumnBy(String columnBy) {
    this.columnBy = columnBy;
  }

  public String getSequenceBy() {
    return sequenceBy;
  }

  public void setSequenceBy(String sequenceBy) {
    this.sequenceBy = sequenceBy;
  }

  public Boolean getDraggable() {
    return draggable;
  }

  public void setDraggable(Boolean draggable) {
    this.draggable = draggable;
  }

  public String getOnNew() {
    return onNew;
  }

  public void setOnNew(String onNew) {
    this.onNew = onNew;
  }

  public String getOnMove() {
    return onMove;
  }

  public void setOnMove(String onMove) {
    this.onMove = onMove;
  }

  public Integer getLimit() {
    return limit;
  }

  public void setLimit(Integer limit) {
    this.limit = limit;
  }

  public Integer getLimitColumns() {
    return limitColumns;
  }

  public void setLimitColumns(Integer limitColumns) {
    this.limitColumns = limitColumns;
  }

  public String getCollapseColumns() {
    return collapseColumns;
  }

  public void setCollapseColumns(String collapseColumns) {
    this.collapseColumns = collapseColumns;
  }

  private Class<?> getModelClass() {
    try {
      return Class.forName(this.getModel());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Invalid Kanban view", e);
    }
  }

  @XmlTransient
  @JsonProperty
  public List<Selection.Option> getColumns() {
    Mapper mapper = Mapper.of(getModelClass());
    Property columnField = mapper.getProperty(columnBy);
    if (columnField == null) {
      throw new RuntimeException("Null field found: " + columnBy);
    }

    if (columnField.isEnum()) {
      return MetaStore.getSelectionList(columnField.getEnumType());
    }

    if (StringUtils.notBlank(columnField.getSelection())) {
      return MetaStore.getSelectionList(columnField.getSelection());
    }

    if (columnField.isReference()) {
      Class<? extends Model> targetClass = columnField.getTarget().asSubclass(Model.class);
      Mapper targetMapper = Mapper.of(targetClass);

      int limit = limitColumns == null ? 12 : limitColumns;
      String orderBy = targetMapper.getProperty("sequence") == null ? null : "sequence";

      return MetaStore.getSelectionList(targetClass, orderBy, limit);
    }

    throw new RuntimeException("Invalid columnBy: " + columnBy);
  }
}
