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
import org.apache.shiro.session.Session;
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
  public AxelorSessionManager(SessionDAO sessionDAO) {
    secureSessionIdCookie = new SimpleCookie(super.getSessionIdCookie());
    secureSessionIdCookie.setSecure(true);
    secureSessionIdCookie.setSameSite(SameSiteOptions.NONE);

    setSessionDAO(sessionDAO);

    // Seconds to milliseconds
    long sessionTimeout =
        AppSettings.get().getInt(AvailableAppSettings.SESSION_TIMEOUT, 60) * 60_000L;
    setGlobalSessionTimeout(sessionTimeout);
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
}
