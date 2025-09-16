/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;
import java.util.Arrays;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
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
