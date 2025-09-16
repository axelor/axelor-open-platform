/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.rpc.filter;

import com.axelor.db.internal.DBHelper;

class LikeFilter extends SimpleFilter {

  private LikeFilter(Operator operator, String fieldName, String value) {
    super(operator, fieldName, value);
  }

  private static String format(Object value) {
    String text = value.toString().toUpperCase();
    if (text.matches("(^%.*)|(.*%$)")) {
      return text;
    }
    return text = "%" + text + "%";
  }

  public static LikeFilter like(String fieldName, Object value) {
    return new LikeFilter(Operator.LIKE, fieldName, format(value));
  }

  public static LikeFilter notLike(String fieldName, Object value) {
    return new LikeFilter(Operator.NOT_LIKE, fieldName, format(value));
  }

  @Override
  public String getQuery() {
    if (DBHelper.isUnaccentEnabled()) {
      return "(unaccent(UPPER(%s)) %s unaccent(?))".formatted(getOperand(), getOperator());
    }
    return "(UPPER(%s) %s ?)".formatted(getOperand(), getOperator());
  }
}
