/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.auth.pac4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.shiro.web.servlet.Cookie;
import org.apache.shiro.web.servlet.Cookie.SameSiteOptions;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.pac4j.core.context.JEEContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.matching.matcher.csrf.CsrfTokenGeneratorMatcher;
import org.pac4j.core.matching.matcher.csrf.DefaultCsrfTokenGenerator;

@Singleton
public class AxelorCsrfMatcher extends CsrfTokenGeneratorMatcher {
  public static final String CSRF_MATCHER_NAME = "axelorCsrfToken";

  private final String cookieName;
  private final String headerName;

  @Inject
  public AxelorCsrfMatcher() {
    this(AuthPac4jModule.CSRF_COOKIE_NAME, AuthPac4jModule.CSRF_HEADER_NAME);
  }

  public AxelorCsrfMatcher(String cookieName, String headerName) {
    super(new DefaultCsrfTokenGenerator());
    this.cookieName = cookieName;
    this.headerName = headerName;
  }

  @Override
  public boolean matches(WebContext context) {
    addResponseCookieAndHeader(context);
    return true;
  }

  protected void addResponseCookieAndHeader(WebContext context) {
    final String token = getCsrfTokenGenerator().get(context);
    final JEEContext jeeContext = ((JEEContext) context);
    final HttpServletRequest request = jeeContext.getNativeRequest();
    final HttpServletResponse response = jeeContext.getNativeResponse();

    String path = request.getContextPath();
    if (path.isEmpty()) {
      path = "/";
    }

    final Cookie cookie = new SimpleCookie(cookieName);
    cookie.setValue(token);
    cookie.setDomain("");
    cookie.setPath(path);
    cookie.setHttpOnly(false);
    if (AuthPac4jInfo.isSecure(request)) {
      cookie.setSecure(true);
      cookie.setSameSite(SameSiteOptions.NONE);
    }
    cookie.saveTo(request, response);

    context.setResponseHeader(headerName, token);
  }
}
