/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.service.menu;

/** Convenience class for a {@link MenuNodeVisitor} */
public class SimpleMenuNodeVisitor implements MenuNodeVisitor {

  @Override
  public MenuNodeResult preChildVisit(MenuNode node) {
    return MenuNodeResult.CONTINUE;
  }

  @Override
  public MenuNodeResult visit(MenuNode node) {
    return MenuNodeResult.CONTINUE;
  }
}
