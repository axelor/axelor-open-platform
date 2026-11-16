/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
