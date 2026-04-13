/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import com.axelor.app.AvailableAppSettings;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionException;
import org.apache.shiro.session.mgt.DefaultSessionKey;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.web.servlet.Cookie;
import org.apache.shiro.web.servlet.Cookie.SameSiteOptions;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.apache.shiro.web.util.WebUtils;

/**
 * Session Manager
 *
 * <p>Uses SameSite attribute for secure requests.
 */
@Singleton
public class AxelorSessionManager extends DefaultWebSessionManager {

  private final Cookie secureSessionIdCookie;
  private final ThreadLocal<HttpServletRequest> currentRequest = new ThreadLocal<>();

  @Inject
  public AxelorSessionManager(
      SessionDAO sessionDAO,
      @Named(AvailableAppSettings.SESSION_TIMEOUT) long sessionTimeoutMinutes) {
    secureSessionIdCookie = new SimpleCookie(super.getSessionIdCookie());
    secureSessionIdCookie.setSecure(true);
    secureSessionIdCookie.setSameSite(SameSiteOptions.NONE);

    setSessionDAO(sessionDAO);

    // Seconds to milliseconds
    long sessionTimeout = sessionTimeoutMinutes * 60_000L;
    setGlobalSessionTimeout(sessionTimeout);

    // Cache is configured with expiry policy, so no session validation scheduler is needed.
    setSessionValidationSchedulerEnabled(false);
  }

  @Override
  protected void onStart(Session session, SessionContext context) {
    currentRequest.set(WebUtils.getHttpRequest(context));
    try {
      super.onStart(session, context);
    } finally {
      currentRequest.remove();
    }
  }

  @Override
  public Cookie getSessionIdCookie() {
    final var request = currentRequest.get();

    if (request == null) {
      return super.getSessionIdCookie();
    }

    var cookie = request.isSecure() ? secureSessionIdCookie : super.getSessionIdCookie();

    if (request.getContextPath().isEmpty()) {
      cookie = new SimpleCookie(cookie);
      cookie.setPath("/");
    }

    return cookie;
  }

  /**
   * Gets session from request and response if it exists.
   *
   * <p>Normally, you can use {@link org.apache.shiro.subject.Subject#getSession}.
   *
   * <p>This is for cases where security manager is not available.
   *
   * @param request
   * @param response
   * @return session or null
   */
  @Nullable
  public Session getSession(HttpServletRequest request, HttpServletResponse response) {
    var sessionId = getSessionId(request, response);
    try {
      return getSession(new DefaultSessionKey(sessionId));
    } catch (SessionException e) {
      return null;
    }
  }
}
