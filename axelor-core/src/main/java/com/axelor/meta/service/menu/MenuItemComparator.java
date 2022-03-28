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
