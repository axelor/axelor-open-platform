/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
package com.axelor.test.db;

import com.axelor.db.EntityHelper;
import javax.persistence.Entity;

@Entity
public class StrategySingleChild extends StrategySingle {

  private Integer myFieldChild = 0;

  public StrategySingleChild() {}

  public Integer getMyFieldChild() {
    return myFieldChild == null ? 0 : myFieldChild;
  }

  public void setMyFieldChild(Integer myFieldChild) {
    this.myFieldChild = myFieldChild;
  }

  @Override
  public boolean equals(Object obj) {
    return EntityHelper.equals(this, obj);
  }

  @Override
  public int hashCode() {
    return 31;
  }

  @Override
  public String toString() {
    return EntityHelper.toString(this);
  }
}
