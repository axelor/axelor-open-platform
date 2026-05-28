/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
