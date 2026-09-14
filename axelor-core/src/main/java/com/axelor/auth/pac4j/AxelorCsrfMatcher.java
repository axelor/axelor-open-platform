/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import com.axelor.auth.pac4j.local.StatelessClient;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.Cookie;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.matching.matcher.csrf.CsrfTokenGenerator;
import org.pac4j.core.matching.matcher.csrf.CsrfTokenGeneratorMatcher;
import org.pac4j.jee.context.JEEContext;

/** CSRF matcher with custom cookie name and header name. */
@Singleton
public class AxelorCsrfMatcher extends CsrfTokenGeneratorMatcher {
  public static final String CSRF_MATCHER_NAME = "axelorCsrfToken";

  private final String cookieName;
  private final String headerName;

  private final Collection<StatelessClient> statelessClients;

  @Inject
  public AxelorCsrfMatcher(
      AxelorCsrfGenerator csrfTokenGenerator, ClientListService clientListService) {
    this(
        csrfTokenGenerator,
        clientListService,
        AuthPac4jModule.CSRF_COOKIE_NAME,
        AuthPac4jModule.CSRF_HEADER_NAME);
  }

  public AxelorCsrfMatcher(
      CsrfTokenGenerator csrfTokenGenerator,
      ClientListService clientListService,
      String cookieName,
      String headerName) {
    super(csrfTokenGenerator);
    this.statelessClients = clientListService.getStatelessClients();
    this.cookieName = cookieName;
    this.headerName = headerName;
  }

  @Override
  public boolean matches(CallContext ctx) {
    var context = ctx.webContext();

    // No CSRF cookie/header for native clients nor stateless clients
    if (!AuthPac4jInfo.isNativeClient(context) && !hasStatelessCredentials(context)) {
      addResponseCookieAndHeader(ctx);
    }
    return true;
  }

  private boolean hasStatelessCredentials(WebContext context) {
    return statelessClients.stream().anyMatch(client -> client.hasCredentials(context));
  }

  /**
   * Adds the CSRF cookie and header to the response.
   *
   * <p>Dynamic secure/samePolicy based on request.isSecure() and uses path from request context
   * path. Those are not supported by default CSRF matcher that uses static config.
   *
   * @param ctx the call context
   */
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
  }
}
