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

import java.util.Arrays;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;
import org.w3c.dom.Node;

@XmlType
@XmlEnum
public enum Position {
  @XmlEnumValue("after")
  AFTER(Node::getParentNode, Node::getNextSibling),

  @XmlEnumValue("before")
  BEFORE(Node::getParentNode, UnaryOperator.identity()),

  @XmlEnumValue("inside")
  INSIDE_LAST(UnaryOperator.identity(), node -> null),

  @XmlEnumValue("inside-first")
  INSIDE_FIRST(UnaryOperator.identity(), Node::getFirstChild);

  private final UnaryOperator<Node> parentNodeFunc;
  private final UnaryOperator<Node> refChildNodeFunc;

  private static final Map<String, Position> POSITION_TYPES =
      Arrays.stream(Position.class.getFields())
          .collect(
              Collectors.toMap(
                  field -> field.getAnnotation(XmlEnumValue.class).value(),
                  field -> {
                    try {
                      return (Position) field.get(Position.class);
                    } catch (IllegalAccessException e) {
                      throw new IllegalArgumentException(e);
                    }
                  }));

  private Position(UnaryOperator<Node> parentNodeFunc, UnaryOperator<Node> refChildNodeFunc) {
    this.parentNodeFunc = parentNodeFunc;
    this.refChildNodeFunc = refChildNodeFunc;
  }

  public void insert(Node node, Node newChild) {
    parentNodeFunc.apply(node).insertBefore(newChild, refChildNodeFunc.apply(node));
  }

  public static Position get(String name) {
    return POSITION_TYPES.getOrDefault(name, AFTER);
  }
}
