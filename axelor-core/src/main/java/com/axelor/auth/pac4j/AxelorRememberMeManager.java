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
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.lang.codec.Base64;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.servlet.Cookie;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RememberMe Manager
 *
 * <p>This implements all of {@link org.apache.shiro.mgt.RememberMeManager} interface and uses
 * SameSite attribute for secure requests.
 */
@Singleton
public class AxelorRememberMeManager extends CookieRememberMeManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(AxelorRememberMeManager.class);

  @Override
  protected void rememberSerializedIdentity(Subject subject, byte[] serialized) {

    if (!WebUtils.isHttp(subject)) {
      if (LOGGER.isDebugEnabled()) {
        String msg =
            "Subject argument is not an HTTP-aware instance.  This is required to obtain a servlet "
                + "request and response in order to set the rememberMe cookie. Returning immediately and "
                + "ignoring rememberMe operation.";
        LOGGER.debug(msg);
      }
      return;
    }

    HttpServletRequest request = WebUtils.getHttpRequest(subject);
    HttpServletResponse response = WebUtils.getHttpResponse(subject);

    // base 64 encode it and store as a cookie:
    String base64 = Base64.encodeToString(serialized);

    // the class attribute is really a template for the outgoing cookies
    Cookie template = getCookie();
    Cookie cookie = new SimpleCookie(template);
    cookie.setValue(base64);
    AxelorSessionManager.updateCookie(cookie, request); // For secure cookie
    cookie.saveTo(request, response);
  }
}
