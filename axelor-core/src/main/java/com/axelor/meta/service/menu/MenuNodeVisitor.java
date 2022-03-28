/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2021 Axelor (<http://axelor.com>).
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
