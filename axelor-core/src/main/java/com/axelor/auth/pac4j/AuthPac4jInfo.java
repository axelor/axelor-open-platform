/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
import com.axelor.inject.Beans;
import com.google.common.base.Preconditions;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.http.client.indirect.IndirectBasicAuthClient;
import org.pac4j.jee.context.JEEContext;

@Singleton
public class AuthPac4jInfo {

  private Set<String> centralClients;

  private String baseUrl;

  private String callbackUrl;

  private String logoutUrl;

  private boolean absoluteUrlRequired;

  private Authenticator authenticator;

  private final Map<String, Map<String, String>> clientInfo = new HashMap<>();

  @Nullable
  public Map<String, String> getClientInfo(String clientName) {
    return clientInfo.get(clientName);
  }

  public void setClientInfo(String clientName, Map<String, String> info) {
    clientInfo.put(clientName, info);
  }

  public void requireAbsoluteUrl() {
    Preconditions.checkArgument(baseUrl == null);
    absoluteUrlRequired = true;
  }

  public String getBaseUrl() {
    if (baseUrl == null) {
      baseUrl = AppSettings.get().getBaseURL();
      if (baseUrl != null && !absoluteUrlRequired) {
        baseUrl = URI.create(baseUrl).getPath();
      }
    }

    return baseUrl;
  }

  public Set<String> getCentralClients() {
    if (centralClients == null) {
      centralClients =
          Beans.get(Clients.class).getClients().stream()
              .filter(IndirectClient.class::isInstance)
              .filter(
                  client ->
                      Stream.of(FormClient.class, IndirectBasicAuthClient.class)
                          .noneMatch(cls -> cls.isInstance(client)))
              .map(Client::getName)
              .collect(Collectors.toCollection(LinkedHashSet::new));
    }
    return centralClients;
  }

  public String getCallbackUrl() {
    if (callbackUrl == null) {
      final AppSettings settings = AppSettings.get();
      String authCallbackUrl = settings.get(AvailableAppSettings.AUTH_CALLBACK_URL, null);
      if (StringUtils.isBlank(authCallbackUrl)) {
        authCallbackUrl = getBaseUrl() + "/callback";
      }
      callbackUrl = authCallbackUrl;
    }
    return callbackUrl;
  }

  public String getLogoutUrl() {
    if (logoutUrl == null) {
      final AppSettings settings = AppSettings.get();
      String authLogoutUrl = settings.get(AvailableAppSettings.AUTH_LOGOUT_DEFAULT_URL, null);
      if (StringUtils.isBlank(authLogoutUrl)) {
        authLogoutUrl = getBaseUrl();
        if (StringUtils.isBlank(authLogoutUrl)) {
          authLogoutUrl = ".";
        }
      }
      logoutUrl = authLogoutUrl;
    }
    return logoutUrl;
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
