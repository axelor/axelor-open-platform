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

import com.axelor.meta.schema.ObjectViews;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.w3c.dom.Node;

@XmlType
@XmlEnum
public enum Position {
  @XmlEnumValue("after")
  AFTER(Node::getParentNode, Node::getNextSibling),

  @XmlEnumValue("before")
  BEFORE(Node::getParentNode, Function.identity()),

  @XmlEnumValue("inside-last")
  INSIDE_LAST(Function.identity(), node -> null),

  @XmlEnumValue("inside-first")
  INSIDE_FIRST(Function.identity(), Node::getFirstChild);

  private final Function<Node, Node> parentNodeFunc;
  private final Function<Node, Node> refChildNodeFunc;

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

  private static final Map<Position, Position> ROOT_NODE_MAP =
      ImmutableMap.of(AFTER, INSIDE_LAST, BEFORE, INSIDE_FIRST);

  private Position(Function<Node, Node> parentNodeFunc, Function<Node, Node> refChildNodeFunc) {
    this.parentNodeFunc = parentNodeFunc;
    this.refChildNodeFunc = refChildNodeFunc;
  }

  public void insert(Node node, Node newChild) {
    parentNodeFunc.apply(node).insertBefore(newChild, refChildNodeFunc.apply(node));
  }

  public static Position get(String name) {
    return POSITION_TYPES.getOrDefault(name, AFTER);
  }

  public static Position get(String name, Node targetNode) {
    final Position position = get(name);

    if (Optional.ofNullable(targetNode.getParentNode())
        .map(Node::getNodeName)
        .orElse("")
        .equals(ObjectViews.class.getAnnotation(XmlRootElement.class).name())) {
      return Position.mapForRootNode(position);
    }

    return position;
  }

  private static Position mapForRootNode(Position position) {
    return ROOT_NODE_MAP.getOrDefault(position, position);
  }
}
