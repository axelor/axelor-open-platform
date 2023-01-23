/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
import java.util.Optional;
import java.util.function.Consumer;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlType
public class EnumType implements BaseType<EnumType> {

  @XmlTransient private String packageName;

  @XmlAttribute(name = "name", required = true)
  private String name;

  @XmlAttribute(name = "numeric")
  private Boolean numeric;

  @XmlElement(name = "item")
  private List<EnumItem> items;

  private static final Logger logger = LoggerFactory.getLogger(EnumType.class);

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
      if (existing == null) {
        items.add(item);
      } else {
        merge(existing, item);
      }
    }
  }

  private void merge(EnumItem existing, EnumItem item) {
    merge(existing.getName(), "title", existing.getTitle(), item.getTitle(), existing::setTitle);
    dontMerge(existing.getName(), "value", existing.getValue(), item.getValue());
    merge(existing.getName(), "help", existing.getHelp(), item.getHelp(), existing::setHelp);
    logger.trace("Merged {}.{}", getName(), existing.getName());
  }

  private <T> void merge(String itemName, String name, T value, T otherValue, Consumer<T> setter) {
    Optional.ofNullable(otherValue)
        .ifPresent(
            v -> {
              setter.accept(v);
              logger.debug(
                  "{}.{}: attribute '{}' is overridden: from '{}' to '{}'",
                  getName(),
                  itemName,
                  name,
                  value,
                  otherValue);
            });
  }

  private <T> void dontMerge(String itemName, String name, T value, T otherValue) {
    Optional.ofNullable(otherValue)
        .ifPresent(
            v ->
                logger.error(
                    "{}.{}: attribute '{}' is not overriddable: from '{}' to '{}'",
                    getName(),
                    itemName,
                    name,
                    value,
                    otherValue));
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
