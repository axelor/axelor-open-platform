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
package com.axelor.auth.pac4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import org.pac4j.core.context.Cookie;
import org.pac4j.core.context.JEEContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.matching.matcher.csrf.CsrfTokenGenerator;
import org.pac4j.core.matching.matcher.csrf.CsrfTokenGeneratorMatcher;

@Singleton
public class AxelorCsrfMatcher extends CsrfTokenGeneratorMatcher {
  public static final String CSRF_MATCHER_NAME = "axelorCsrfToken";

  private final String cookieName;
  private final String headerName;

  @Inject
  public AxelorCsrfMatcher(AxelorCsrfGenerator csrfTokenGenerator) {
    this(csrfTokenGenerator, AuthPac4jModule.CSRF_COOKIE_NAME, AuthPac4jModule.CSRF_HEADER_NAME);
  }

  public AxelorCsrfMatcher(
      CsrfTokenGenerator csrfTokenGenerator, String cookieName, String headerName) {
    super(csrfTokenGenerator);
    this.cookieName = cookieName;
    this.headerName = headerName;
  }

  @Override
  public boolean matches(WebContext context, SessionStore sessionStore) {
    addResponseCookieAndHeader(context, sessionStore);
    return true;
  }

  protected void addResponseCookieAndHeader(WebContext context, SessionStore sessionStore) {
    final String token = getCsrfTokenGenerator().get(context, sessionStore);
    final JEEContext jeeContext = ((JEEContext) context);
    final HttpServletRequest request = jeeContext.getNativeRequest();
    final String contextPath = request.getContextPath();

    final var cookie = new Cookie(cookieName, token);
    cookie.setDomain("");
    cookie.setPath(contextPath.isEmpty() ? "/" : contextPath);
    cookie.setHttpOnly(false);
    if (request.isSecure()) {
      cookie.setSecure(true);
      cookie.setSameSitePolicy("None");
    }

    context.addResponseCookie(cookie);
    context.setResponseHeader(headerName, token);
    //    context.setRequestAttribute(Pac4jConstants.CSRF_TOKEN, token); // XXX
  }
}
