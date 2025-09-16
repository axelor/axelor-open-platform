/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.rpc.filter;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class JPQLFilter extends Filter {

  private static final Pattern BLACKLIST_PATTERN =
      AppSettings.get().get(AvailableAppSettings.APPLICATION_DOMAIN_BLOCKLIST_PATTERN, null) == null
          ? null
          : Pattern.compile(
              AppSettings.get()
                  .get(AvailableAppSettings.APPLICATION_DOMAIN_BLOCKLIST_PATTERN)
                  .trim(),
              Pattern.CASE_INSENSITIVE);

  private String jpql;

  private Object[] params;

  public JPQLFilter(String jpql, Object... params) {
    this.jpql = jpql;
    this.params = params;
  }

  public static JPQLFilter forDomain(String jpql, Object... params) {
    if (BLACKLIST_PATTERN != null && BLACKLIST_PATTERN.matcher(jpql).find()) {
      throw new IllegalArgumentException("Invalid domain, filter uses blacklisted keywords.");
    }
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
