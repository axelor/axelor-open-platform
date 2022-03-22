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

import static com.axelor.tools.code.entity.model.Utils.isTrue;
import static com.axelor.tools.code.entity.model.Utils.notBlank;

import com.axelor.tools.code.JavaAnnotation;
import com.axelor.tools.code.JavaField;
import com.axelor.tools.code.JavaMethod;
import com.axelor.tools.code.JavaType;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class EnumType implements BaseType<EnumType> {

  @XmlTransient private String packageName;

  @XmlAttribute(name = "name", required = true)
  private String name;

  @XmlAttribute(name = "numeric")
  private Boolean numeric;

  @XmlElement(name = "item")
  private List<EnumItem> items;

  void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
    final Namespace ns = ((DomainModels) parent).getNamespace();
    this.packageName = ns.getPackageName();
  }

  public String getPackageName() {
    return packageName;
  }

  public String getName() {
    return name;
  }

  public void setName(String value) {
    this.name = value;
  }

  public Boolean getNumeric() {
    return numeric;
  }

  public void setNumeric(Boolean value) {
    this.numeric = value;
  }

  public List<EnumItem> getItems() {
    if (items == null) {
      items = new ArrayList<>();
    }
    return this.items;
  }

  private boolean isCompatible(EnumItem existing, EnumItem item) {
    return existing == null || Objects.equals(existing.getName(), item.getName());
  }

  public EnumItem findItem(String name) {
    return getItems().stream()
        .filter(p -> Objects.equals(p.getName(), name))
        .findFirst()
        .orElse(null);
  }

  @Override
  public void merge(EnumType other) {
    for (EnumItem item : other.items) {
      EnumItem existing = findItem(item.getName());
      if (isCompatible(existing, item)) {
        if (existing != null) {
          items.remove(existing);
        }
        items.add(item);
      }
    }
  }

  @Override
  public JavaType toJavaClass() {
    String type = isTrue(numeric) ? "Integer" : "String";
    JavaType pojo = JavaType.newEnum(getName(), Modifier.PUBLIC);

    pojo.superInterface("com.axelor.db.ValueEnum<" + type + ">");

    if (items == null) {
      return pojo;
    }

    items.forEach(item -> pojo.enumConstant(item.toJavaEnumConstant()));

    boolean isValueEnum = items.stream().anyMatch(item -> notBlank(item.getValue()));

    pojo.method(
        new JavaMethod("getValue", type, Modifier.PUBLIC)
            .annotation(new JavaAnnotation("Override"))
            .code(isValueEnum ? "return value;" : "return name();"));

    if (isValueEnum) {
      pojo.field(new JavaField("value", type, Modifier.PRIVATE | Modifier.FINAL));
      pojo.constructor(
          new JavaMethod(name, null, Modifier.PRIVATE)
              .param("value", type)
              .code("this.value = {0:t}.requireNonNull(value);", "java.util.Objects"));
    }

    return pojo;
  }
}
