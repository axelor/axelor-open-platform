/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.service.menu;

import com.axelor.meta.schema.views.MenuItem;
import java.util.Comparator;

public class MenuItemComparator implements Comparator<MenuItem> {

  @Override
  public int compare(MenuItem o1, MenuItem o2) {
    Integer n = o1.getOrder();
    Integer m = o2.getOrder();

    if (n == null) n = 0;
    if (m == null) m = 0;

    return Integer.compare(n, m);
  }
}
