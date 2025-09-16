/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.Cookie;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.matching.matcher.csrf.CsrfTokenGenerator;
import org.pac4j.core.matching.matcher.csrf.CsrfTokenGeneratorMatcher;
import org.pac4j.jee.context.JEEContext;

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
  public boolean matches(CallContext ctx) {
    // No CSRF cookie/header for native clients
    if (!AuthPac4jInfo.isNativeClient(ctx.webContext())) {
      addResponseCookieAndHeader(ctx);
    }
    return true;
  }

  protected void addResponseCookieAndHeader(CallContext ctx) {
    final WebContext context = ctx.webContext();
    final SessionStore sessionStore = ctx.sessionStore();
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
