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

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;
import org.w3c.dom.Node;

@XmlType
@XmlEnum
public enum PositionType {
  @XmlEnumValue("inside")
  INSIDE(node -> null),
  @XmlEnumValue("after")
  AFTER(node -> node != null ? node.getNextSibling() : node),
  @XmlEnumValue("before")
  BEFORE(node -> node);

  private final String value;
  private final Function<Node, Node> refNodeFunc;

  private static final Map<String, PositionType> POSITION_TYPES =
      Arrays.stream(PositionType.class.getFields())
          .collect(
              Collectors.toMap(
                  field -> field.getAnnotation(XmlEnumValue.class).value(),
                  field -> {
                    try {
                      return (PositionType) field.get(PositionType.class);
                    } catch (IllegalAccessException e) {
                      throw new RuntimeException(e);
                    }
                  }));
  private static final Set<String> DEFAULT_TO_AFTER_TAGS =
      Sets.newHashSet(
          "button", "field", "help", "hilite", "label", "static", "spacer", "separator");

  private PositionType(Function<Node, Node> refNodeFunc) {
    value = name().toLowerCase();
    this.refNodeFunc = refNodeFunc;
  }

  public String getValue() {
    return value;
  }

  public Function<Node, Node> getRefNodeFunc() {
    return refNodeFunc;
  }

  public static PositionType get(String name) {
    return POSITION_TYPES.computeIfAbsent(
        name,
        key -> {
          throw new NoSuchElementException(name);
        });
  }

  public static PositionType get(String name, String tagName) {
    if (name.isEmpty()) {
      return DEFAULT_TO_AFTER_TAGS.contains(tagName) ? AFTER : INSIDE;
    }

    return get(name);
  }
}
