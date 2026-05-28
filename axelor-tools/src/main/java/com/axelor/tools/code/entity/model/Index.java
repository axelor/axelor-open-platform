/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.code.entity.model;

import static com.axelor.tools.code.entity.model.Utils.*;

import com.axelor.tools.code.JavaAnnotation;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import java.util.stream.Collectors;

@XmlType
public class Index {

  @XmlAttribute(name = "name")
  private String name;

  @XmlAttribute(name = "columns", required = true)
  private String columns;

  @XmlAttribute(name = "unique")
  private Boolean unique;

  public String getName() {
    return name;
  }

  public void setName(String value) {
    this.name = value;
  }

  public String getColumns() {
    return columns;
  }

  public void setColumns(String value) {
    this.columns = value;
  }

  public Boolean getUnique() {
    return unique;
  }

  public void setUnique(Boolean unique) {
    this.unique = unique;
  }

  protected String getColumn(Entity entity, String indexColumn) {
    String[] parts = indexColumn.split("\\s+");
    String field = parts[0];
    String orderBy = parts.length > 1 ? " " + parts[1] : "";

    Property property = entity.findField(field);
    if (property == null) {
      return indexColumn;
    }

    String column = property.getColumn();
    if (notBlank(column)) return column + orderBy;
    if (property.isReference()) return property.getColumnAuto() + orderBy;
    return indexColumn;
  }

  protected String getColumnList(Entity entity) {
    return list(getColumns()).stream()
        .map(column -> getColumn(entity, column))
        .collect(Collectors.joining(", "));
  }

  public JavaAnnotation toJavaAnnotation(Entity entity) {
    JavaAnnotation annotation =
        new JavaAnnotation("jakarta.persistence.Index")
            .param("columnList", "{0:s}", getColumnList(entity));
    if (notBlank(name)) {
      annotation.param("name", "{0:s}", name);
    }
    if (isTrue(unique)) {
      annotation.param("unique", "true");
    }
    return annotation;
  }
}
