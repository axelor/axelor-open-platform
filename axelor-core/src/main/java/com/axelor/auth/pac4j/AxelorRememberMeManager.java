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

import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.servlet.Cookie;
import org.apache.shiro.web.servlet.Cookie.SameSiteOptions;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.util.WebUtils;

/**
 * RememberMe Manager
 *
 * <p>This implements all of {@link org.apache.shiro.mgt.RememberMeManager} interface and uses
 * SameSite attribute for secure requests.
 */
@Singleton
public class AxelorRememberMeManager extends CookieRememberMeManager {

  private final Cookie secureCookie;
  private final ThreadLocal<HttpServletRequest> currentRequest = new ThreadLocal<>();

  public AxelorRememberMeManager() {
    secureCookie = new SimpleCookie(super.getCookie());
    secureCookie.setSecure(true);
    secureCookie.setSameSite(SameSiteOptions.NONE);
  }

  @Override
  protected void rememberSerializedIdentity(Subject subject, byte[] serialized) {
    currentRequest.set(WebUtils.getHttpRequest(subject));
    try {
      super.rememberSerializedIdentity(subject, serialized);
    } finally {
      currentRequest.remove();
    }
  }

  @Override
  public Cookie getCookie() {
    final var request = currentRequest.get();

    if (request == null) {
      return super.getCookie();
    }

    var cookie = request.isSecure() ? secureCookie : super.getCookie();

    if (request.getContextPath().isEmpty()) {
      cookie = new SimpleCookie(cookie);
      cookie.setPath("/");
    }

    return cookie;
  }
}
