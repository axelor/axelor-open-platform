/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
package com.axelor.app.internal;

import com.axelor.app.AppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
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
      final String appLocale = AppSettings.get().get("application.locale", null);
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

  private String getHeader(ServletRequest req, String name, String defaultValue) {
    final String value = ((HttpServletRequest) req).getHeader(name);
    return StringUtils.isBlank(value) ? defaultValue : value;
  }

  private String getBaseUrl(ServletRequest req) {
    final String proto = getHeader(req, "X-Forwarded-Proto", req.getScheme());
    final String port = getHeader(req, "X-Forwarded-Port", "" + req.getServerPort());
    final String host = getHeader(req, "X-Forwarded-Host", req.getServerName());
    final String context =
        getHeader(req, "X-Forwarded-Context", req.getServletContext().getContextPath());
    return port.equals("80") || port.equals("443")
        ? proto + "://" + host + context
        : proto + "://" + host + ":" + port + context;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    BASE_URL.set(getBaseUrl(request));
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
