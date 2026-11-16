/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2026 Axelor (<http://axelor.com>).
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

import java.util.Arrays;
import java.util.List;

public class JPQLFilter extends Filter {

  private String jpql;

  private Object[] params;

  public JPQLFilter(String jpql, Object... params) {
    this.jpql = jpql;
    this.params = params;
  }

  /**
   * A filter from a domain the client may have written.
   *
   * <p>The domain is joined to other filters, the permission filter among them, as text, so it is
   * validated by {@link DomainValidator} first: a domain such as {@code 1=1)) OR ((1=1} would
   * otherwise close the parentheses it is wrapped in and OR its way past whatever is ANDed after
   * it.
   */
  public static JPQLFilter forDomain(String jpql, Object... params) {
    DomainValidator.validate(jpql);
    return new JPQLFilter(jpql, params);
  }

  @Override
  public String getQuery() {
    return "(" + this.jpql + ")";
  }

  @Override
  public List<Object> getParams() {
    return Arrays.asList(this.params);
  }
}
