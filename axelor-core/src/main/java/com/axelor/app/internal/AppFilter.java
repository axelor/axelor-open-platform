/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.app.internal;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.google.inject.Singleton;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Locale;

@Singleton
public class AppFilter implements Filter {

  private static final ThreadLocal<String> BASE_URL = new ThreadLocal<>();
  private static final ThreadLocal<Locale> LANGUAGE = new ThreadLocal<>();

  private static Locale APP_LOCALE;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    try {
      final String appLocale = AppSettings.get().get(AvailableAppSettings.APPLICATION_LOCALE, null);
      APP_LOCALE = appLocale == null ? null : Locale.forLanguageTag(appLocale);
    } catch (Exception e) {
    }
  }

  public static String getBaseURL() {
    return BASE_URL.get();
  }

  public static void setBaseURL(String baseUrl) {
    BASE_URL.set(baseUrl);
  }

  public static Locale getLanguage() {
    return LANGUAGE.get();
  }

  public static void setLanguage(Locale language) {
    LANGUAGE.set(language);
  }

  public static Locale getLocale() {
    User user = AuthUtils.getUser();
    if (user != null && user.getLanguage() != null) {
      return Locale.forLanguageTag(user.getLanguage());
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
