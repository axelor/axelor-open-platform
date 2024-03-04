/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
import com.axelor.auth.pac4j.local.AxelorAuthenticator;
import com.axelor.common.StringUtils;
import com.axelor.common.UriBuilder;
import com.axelor.inject.Beans;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.jee.context.JEEContext;

@Singleton
public class AuthPac4jInfo {

  private Authenticator authenticator;

  public String getBaseUrl() {
    return AppSettings.get().getBaseURL();
  }

  /**
   * Return the main callback endpoint for users authentication
   *
   * @return url form where the users are authenticated
   */
  public String getCallbackUrl() {
    String authCallbackUrl = AppSettings.get().get(AvailableAppSettings.AUTH_CALLBACK_URL, null);
    if (StringUtils.isBlank(authCallbackUrl)) {
      authCallbackUrl = UriBuilder.from(getBaseUrl()).addPath("/callback").toUri().toString();
    }
    return authCallbackUrl;
  }

  /**
   * Return the default post logout url.
   *
   * @return url where the users will be redirected after logout
   */
  public String getLogoutUrl() {
    final AppSettings settings = AppSettings.get();
    String authLogoutUrl = settings.get(AvailableAppSettings.AUTH_LOGOUT_DEFAULT_URL, null);
    if (StringUtils.isBlank(authLogoutUrl)) {
      authLogoutUrl = getBaseUrl();
      if (StringUtils.isBlank(authLogoutUrl)) {
        authLogoutUrl = ".";
      }
    }
    return authLogoutUrl;
  }

  public void setAuthenticator(Authenticator authenticator) {
    this.authenticator = authenticator;
  }

  public Authenticator getAuthenticator() {
    if (authenticator == null) {
      authenticator = Beans.get(AxelorAuthenticator.class);
    }

    return authenticator;
  }

  public static boolean isXHR(WebContext context) {
    return context instanceof JEEContext && isXHR(((JEEContext) context).getNativeRequest());
  }

  public static boolean isXHR(HttpServletRequest request) {
    return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))
        || "application/json".equals(request.getHeader("Accept"))
        || "application/json".equals(request.getHeader("Content-Type"));
  }

  public static boolean isNativeClient(WebContext context) {
    return context.getRequestHeader("Origin").isEmpty();
  }
}
