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
package com.axelor.app.internal;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Locale;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

@Singleton
public class AppFilter implements Filter {

  private static final ThreadLocal<String> BASE_URL = new ThreadLocal<>();
  private static final ThreadLocal<Locale> LANGUAGE = new ThreadLocal<>();

  private static Locale APP_LOCALE;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    try {
      final String appLocale = AppSettings.get().get(AvailableAppSettings.APPLICATION_LOCALE, null);
      APP_LOCALE = appLocale == null ? null : new Locale(appLocale);
    } catch (Exception e) {
    }
  }

  public static String getBaseURL() {
    return BASE_URL.get();
  }

  public static Locale getLocale() {
    User user = AuthUtils.getUser();
    if (user != null && user.getLanguage() != null) {
      return new Locale(user.getLanguage());
    }
    if (user != null && APP_LOCALE != null) {
      return APP_LOCALE;
    }
    if (LANGUAGE.get() == null) {
      return Locale.getDefault();
    }
    return LANGUAGE.get();
  }

  private String computeBaseUrl(ServletRequest req) {
    final String proto = req.getScheme();
    final int port = req.getServerPort();
    final String host = req.getServerName();
    final String context = ((HttpServletRequest) req).getContextPath();
    return port == 80 || port == 443
        ? proto + "://" + host + context
        : proto + "://" + host + ":" + port + context;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    BASE_URL.set(computeBaseUrl(request));
    LANGUAGE.set(request.getLocale());
    try {
      chain.doFilter(request, response);
    } finally {
      LANGUAGE.remove();
      BASE_URL.remove();
    }
  }

  @Override
  public void destroy() {
    LANGUAGE.remove();
    BASE_URL.remove();
  }
}
