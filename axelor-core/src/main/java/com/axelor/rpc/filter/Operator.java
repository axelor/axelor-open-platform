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
package com.axelor.rpc.filter;

import com.google.common.base.CaseFormat;

public enum Operator {
  AND("AND"),

  OR("OR"),

  NOT("NOT"),

  EQUALS("="),

  NOT_EQUAL("!="),

  LESS_THAN("<"),

  GREATER_THAN(">"),

  LESS_OR_EQUAL("<="),

  GREATER_OR_EQUAL(">="),

  LIKE("LIKE"),

  NOT_LIKE("NOT LIKE"),

  IS_NULL("IS NULL"),

  NOT_NULL("IS NOT NULL"),

  IN("IN"),

  NOT_IN("NOT IN"),

  BETWEEN("BETWEEN"),

  NOT_BETWEEN("NOT BETWEEN");

  private String value;

  private String id;

  private Operator(String value) {
    this.value = value;
    this.id = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, this.name());
  }

  public static Operator get(String name) {
    for (Operator operator : Operator.values()) {
      if (operator.value.equals(name) || operator.id.equals(name)) {
        return operator;
      }
    }
    throw new IllegalArgumentException("No such operator: " + name);
  }

  @Override
  public String toString() {
    return value;
  }
}
