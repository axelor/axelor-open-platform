/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.rpc.filter;

import com.axelor.db.hibernate.type.JsonFunction;
import jakarta.persistence.PersistenceException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

class SimpleFilter extends Filter {

  private static Pattern NAME_PATTERN = Pattern.compile("\\w+(\\.\\w+)*");

  private String fieldName;

  private Operator operator;

  private Object value;

  public SimpleFilter(Operator operator, String fieldName, Object value) {
    this.fieldName = fieldName;
    this.operator = operator;
    this.value = value;
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public Operator getOperator() {
    return operator;
  }

  public void setOperator(Operator operator) {
    this.operator = operator;
  }

  protected String getOperand() {
    final String name = getFieldName();
    if (name.indexOf("::") > -1) {
      return JsonFunction.fromPath(name).toString();
    }

    if (!NAME_PATTERN.matcher(name).matches()) {
      throw new PersistenceException("Invalid field name: " + name);
    }

    return "self." + name;
  }

  @Override
  public String getQuery() {
    return "(%s %s ?)".formatted(getOperand(), operator);
  }

  @Override
  public List<Object> getParams() {
    List<Object> params = new ArrayList<>();
    params.add(value);
    return params;
  }
}
