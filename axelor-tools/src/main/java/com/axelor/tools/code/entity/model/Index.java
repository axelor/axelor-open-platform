/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.tools.code.entity.model;

import static com.axelor.tools.code.entity.model.Utils.*;

import com.axelor.tools.code.JavaAnnotation;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class Index {

  @XmlAttribute(name = "name")
  private String name;

  @XmlAttribute(name = "columns", required = true)
  private String columns;

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

  protected String getColumn(Property property, String column) {
    if (property == null) return column;
    String col = property.getColumn();
    if (notBlank(col)) return col;
    if (property.isReference()) return property.getColumnAuto();
    return column;
  }

  protected String getColumnList(Entity entity) {
    return list(getColumns()).stream()
        .map(column -> getColumn(entity.findField(column), column))
        .collect(Collectors.joining(", "));
  }

  public JavaAnnotation toJavaAnnotation(Entity entity) {
    JavaAnnotation annotation =
        new JavaAnnotation("javax.persistence.Index")
            .param("columnList", "{0:s}", getColumnList(entity));
    if (notBlank(name)) {
      annotation.param("name", "{0:s}", name);
    }
    return annotation;
  }
}
