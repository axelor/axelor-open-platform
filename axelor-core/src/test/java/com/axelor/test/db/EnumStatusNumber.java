/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
package com.axelor.test.db;

import com.axelor.db.ValueEnum;
import java.util.Objects;

public enum EnumStatusNumber implements ValueEnum<Integer> {
  ONE(1),

  TWO(2),

  THREE(3);

  private final Integer value;

  private EnumStatusNumber(Integer value) {
    this.value = Objects.requireNonNull(value);
  }

  @Override
  public Integer getValue() {
    return value;
  }
}
