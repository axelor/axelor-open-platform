/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.service.menu;

/** {@link MenuNode} visitor interface */
public interface MenuNodeVisitor {

  /** Visitor decision */
  enum MenuNodeResult {

    /** Continue processing the tree */
    CONTINUE,
    /** Skip the child nodes */
    TERMINATE
  }

  /**
   * Invoke before a child node is visited
   *
   * @param node node to visit
   * @return MenuNodeResult
   */
  MenuNodeResult preChildVisit(MenuNode node);

  /**
   * Invoke when visiting a node
   *
   * @param node visiting node
   * @return MenuNodeResult
   */
  MenuNodeResult visit(MenuNode node);
}
