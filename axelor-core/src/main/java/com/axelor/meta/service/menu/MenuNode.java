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
package com.axelor.meta.service.menu;

import com.axelor.common.ObjectUtils;
import com.axelor.meta.db.MetaMenu;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MenuNode {

  private static final Logger LOG = LoggerFactory.getLogger(MenuNode.class);

  private final List<MenuNode> children = new ArrayList<>();
  private MenuNode parent = null;
  private MetaMenu data = null;

  public MenuNode(MetaMenu data) {
    this.data = data;
  }

  public MenuNode(MetaMenu data, MenuNode parent) {
    this.data = data;
    this.parent = parent;
  }

  public List<MenuNode> getChildren() {
    return children;
  }

  public void addChild(MetaMenu data) {
    MenuNode child = new MenuNode(data, this);
    this.children.add(child);
  }

  public MetaMenu getMetaMenu() {
    return this.data;
  }

  public boolean isRoot() {
    return (this.data == null);
  }

  public boolean isLeaf() {
    return ObjectUtils.isEmpty(this.children);
  }

  public void traverse(MenuNodeVisitor visitor) {
    MenuNodeVisitor.MenuNodeResult visitResult = visitor.visit(this);
    if (visitResult == MenuNodeVisitor.MenuNodeResult.TERMINATE) {
      return;
    }
    if (!isLeaf()) {
      Iterator<MenuNode> it = children.iterator();
      while (it.hasNext()) {
        MenuNode child = it.next();
        MenuNodeVisitor.MenuNodeResult preVisitResult = visitor.preChildVisit(child);
        if (preVisitResult == MenuNodeVisitor.MenuNodeResult.TERMINATE) {
          continue;
        }
        child.traverse(visitor);
      }
    }
  }

  /**
   * Build N-ary Tree from list of meta menus
   *
   * @param metaMenus list of meta menus
   * @return N-ary Tree
   */
  public static MenuNode buildTree(List<MetaMenu> metaMenus) {
    MenuNode rootNode = new MenuNode(null);

    List<MetaMenu> menusToProcess = new ArrayList<>(metaMenus);
    int nbrOfMenusToProcess = 0;
    while (nbrOfMenusToProcess != menusToProcess.size() && !menusToProcess.isEmpty()) {
      nbrOfMenusToProcess = menusToProcess.size();
      Iterator<MetaMenu> iterator = menusToProcess.iterator();
      while (iterator.hasNext()) {
        MetaMenu next = iterator.next();
        MenuNode parent = rootNode.searchNode(next.getParent());
        if (parent == null) {
          continue;
        }
        parent.addChild(next);
        iterator.remove();
      }
    }

    if (!menusToProcess.isEmpty()) {
      LOG.warn(
          "Some menus can't be processed. For the following menus, check the parent references : {}",
          menusToProcess.stream().map(MetaMenu::getName).collect(Collectors.joining(", ")));
    }

    return rootNode;
  }

  /**
   * Search for a node with the given {@link MetaMenu}. It searches in the current node, in all
   * children, their children's children and so on
   *
   * @param target the meta menu to search
   * @return the found node
   */
  public MenuNode searchNode(MetaMenu target) {
    if (target == null) {
      // If no parent, attach to the current node (ie, the root node)
      return this;
    }
    if (!isRoot() && getMetaMenu().getName().equals(target.getName())) {
      return this;
    }
    for (MenuNode child : getChildren()) {
      MenuNode subNode = child.searchNode(target);
      if (subNode != null) {
        return subNode;
      }
    }
    return null;
  }
}
