/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.rpc.filter;

import java.util.Collections;
import java.util.List;

class NullFilter extends SimpleFilter {

  private NullFilter(Operator operator, String fieldName, Object value) {
    super(operator, fieldName, value);
  }

  public static NullFilter isNull(String fieldName) {
    return new NullFilter(Operator.IS_NULL, fieldName, null);
  }

  public static NullFilter notNull(String fieldName) {
    return new NullFilter(Operator.NOT_NULL, fieldName, null);
  }

  @Override
  public String getQuery() {
    return String.format("(%s %s)", getOperand(), getOperator());
  }

  @Override
  public List<Object> getParams() {
    return Collections.emptyList();
  }
}
