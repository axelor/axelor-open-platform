/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.pac4j.local.AxelorAuthenticator;
import com.axelor.common.StringUtils;
import com.axelor.common.UriBuilder;
import com.axelor.inject.Beans;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.jee.context.JEEContext;
import org.pac4j.jee.context.JEEFrameworkParameters;

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
      authCallbackUrl =
          UriBuilder.from(Optional.ofNullable(getBaseUrl()).orElse(""))
              .addPath("/callback")
              .toUri()
              .toString();
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

  public Optional<UserProfile> getUserProfile(ServletRequest request, ServletResponse response) {
    final var config = Beans.get(Config.class);
    final var webContextFactory = config.getWebContextFactory();
    final var sessionStoreFactory = config.getSessionStoreFactory();
    final var profileManagerFactory = config.getProfileManagerFactory();

    final var parameters =
        new JEEFrameworkParameters((HttpServletRequest) request, (HttpServletResponse) response);

    final var context = webContextFactory.newContext(parameters);
    final var sessionStore = sessionStoreFactory.newSessionStore(parameters);
    final var profileManager = profileManagerFactory.apply(context, sessionStore);

    return profileManager.getProfile();
  }

  public static boolean isXHR(CallContext ctx) {
    return isXHR(ctx.webContext());
  }

  public static boolean isXHR(WebContext context) {
    return context instanceof JEEContext jeeContext && isXHR(jeeContext.getNativeRequest());
  }

  public static boolean isXHR(HttpServletRequest request) {
    return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))
        || "application/json".equals(request.getHeader("Accept"))
        || "application/json".equals(request.getHeader("Content-Type"));
  }

  public static boolean isNativeClient(WebContext context) {
    return context.getRequestHeader("Origin").isEmpty();
  }

  public static boolean isWebSocket(HttpServletRequest request) {
    return "websocket".equals(request.getHeader("Upgrade"));
  }
}
