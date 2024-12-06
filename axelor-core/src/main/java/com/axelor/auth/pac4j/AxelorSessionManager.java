/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.Serializable;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.web.servlet.Cookie;
import org.apache.shiro.web.servlet.Cookie.SameSiteOptions;
import org.apache.shiro.web.servlet.ShiroHttpServletRequest;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Session Manager
 *
 * <p>Uses SameSite attribute for secure requests.
 */
@Singleton
public class AxelorSessionManager extends DefaultWebSessionManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(AxelorSessionManager.class);

  @Inject
  public AxelorSessionManager(SessionDAO sessionDAO) {
    setSessionDAO(sessionDAO);

    // Seconds to milliseconds
    long sessionTimeout =
        AppSettings.get().getInt(AvailableAppSettings.SESSION_TIMEOUT, 60) * 60_000L;
    setGlobalSessionTimeout(sessionTimeout);
  }

  /**
   * Stores session ID in a cookie.
   *
   * <p>See {@link org.apache.shiro.web.session.mgt.DefaultWebSessionManager#storeSessionId()}
   *
   * @param currentId session id
   * @param request HTTP request
   * @param response HTTP response
   */
  private void storeSecureSessionId(
      Serializable currentId, HttpServletRequest request, HttpServletResponse response) {
    if (currentId == null) {
      String msg = "sessionId cannot be null when persisting for subsequent requests.";
      throw new IllegalArgumentException(msg);
    }
    Cookie template = getSessionIdCookie();
    Cookie cookie = new SimpleCookie(template);
    String idString = currentId.toString();
    cookie.setValue(idString);
    updateCookie(cookie, request); // For secure cookie
    cookie.saveTo(request, response);
    LOGGER.trace("Set session ID cookie for session with id {}", idString);
  }

  /**
   * Stores the Session's ID, usually as a Cookie, to associate with future requests.
   *
   * <p>See {@link org.apache.shiro.web.session.mgt.DefaultWebSessionManager#onStart()}
   *
   * @param session the session that was just {@link #createSession created}.
   */
  @Override
  protected void onStart(Session session, SessionContext context) {
    if (!WebUtils.isHttp(context)) {
      LOGGER.debug(
          "SessionContext argument is not HTTP compatible or does not have an HTTP request/response "
              + "pair. No session ID cookie will be set.");
      return;
    }
    HttpServletRequest request = WebUtils.getHttpRequest(context);
    HttpServletResponse response = WebUtils.getHttpResponse(context);

    if (isSessionIdCookieEnabled()) {
      Serializable sessionId = session.getId();
      storeSecureSessionId(sessionId, request, response); // For secure cookie
    } else {
      LOGGER.debug(
          "Session ID cookie is disabled.  No cookie has been set for new session with id {}",
          session.getId());
    }

    request.removeAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID_SOURCE);
    request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_IS_NEW, Boolean.TRUE);
  }

  protected static void updateCookie(Cookie cookie, HttpServletRequest request) {
    if (request.isSecure()) {
      cookie.setSecure(true);
      cookie.setSameSite(SameSiteOptions.NONE);
    }

    if (request.getContextPath().isEmpty()) {
      cookie.setPath("/");
    }
  }
}
