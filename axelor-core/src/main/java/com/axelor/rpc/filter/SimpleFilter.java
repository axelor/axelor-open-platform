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

import com.axelor.db.hibernate.type.JsonFunction;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.persistence.PersistenceException;

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
    return String.format("(%s %s ?)", getOperand(), operator);
  }

  @Override
  public List<Object> getParams() {
    List<Object> params = new ArrayList<Object>();
    params.add(value);
    return params;
  }
}
