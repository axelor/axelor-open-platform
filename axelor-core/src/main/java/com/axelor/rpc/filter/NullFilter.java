/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
    return "(%s %s)".formatted(getOperand(), getOperator());
  }

  @Override
  public List<Object> getParams() {
    return Collections.emptyList();
  }
}
