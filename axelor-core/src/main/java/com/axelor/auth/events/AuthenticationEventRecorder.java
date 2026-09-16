/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.events;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.AuthenticationEvent;
import com.axelor.auth.db.AuthenticationStatus;
import com.axelor.auth.db.User;
import com.axelor.auth.pac4j.ClientListService;
import com.axelor.auth.pac4j.ClientTitles;
import com.axelor.auth.pac4j.local.MfaAuthenticator;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.HttpHeaders;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.shiro.web.util.WebUtils;
import org.pac4j.core.client.Client;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.ldap.profile.LdapProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Records successful authentications as {@link AuthenticationEvent}.
 *
 * <p>Attempts made with direct clients are not recorded, as they authenticate every request.
 */
@Singleton
public class AuthenticationEventRecorder {

  public static final String DETAIL_CLIENT = "client";
  public static final String DETAIL_IDENTIFIER = "identifier";
  public static final String DETAIL_USER_AGENT = "userAgent";
  public static final String DETAIL_MFA_METHOD = "mfaMethod";

  private static final Logger log = LoggerFactory.getLogger(AuthenticationEventRecorder.class);

  private static final int MAX_USER_AGENT_LENGTH = 255;

  private static final String LDAP_TITLE = "LDAP";

  private final Provider<ClientListService> clientListService;
  private final ObjectMapper objectMapper;

  @Inject
  public AuthenticationEventRecorder(
      Provider<ClientListService> clientListService, ObjectMapper objectMapper) {
    this.clientListService = clientListService;
    this.objectMapper = objectMapper;
  }

  /**
   * Records a successful authentication.
   *
   * @param user the authenticated user
   * @param profile the authenticated profile, may be {@code null} if unknown
   */
  public void onLoginSuccess(User user, CommonProfile profile) {
    try {
      final String clientName = profile != null ? profile.getClientName() : null;

      if (!accept(clientName)) {
        return;
      }

      final AuthenticationEvent authEvent = new AuthenticationEvent();
      authEvent.setCreatedOn(LocalDateTime.now());
      authEvent.setUser(user);
      authEvent.setProvider(getProviderTitle(clientName, profile));
      authEvent.setStatus(AuthenticationStatus.SUCCESS);

      final Map<String, Object> details = new LinkedHashMap<>();
      putDetail(details, DETAIL_CLIENT, clientName);
      putDetail(details, DETAIL_IDENTIFIER, user.getCode());
      putDetail(details, DETAIL_MFA_METHOD, getMfaMethod(profile));

      final HttpServletRequest httpRequest = WebUtils.getHttpRequest(AuthUtils.getSubject());
      if (httpRequest != null) {
        authEvent.setIpAddress(httpRequest.getRemoteAddr());
        putDetail(
            details,
            DETAIL_USER_AGENT,
            StringUtils.truncate(
                httpRequest.getHeader(HttpHeaders.USER_AGENT), MAX_USER_AGENT_LENGTH));
      }

      authEvent.setDetails(toJson(details));

      JPA.runInTransaction(() -> JPA.save(authEvent));
    } catch (Exception e) {
      // never let auditing break the login
      log.error("Unable to record authentication event", e);
    }
  }

  /**
   * Returns a human-readable name of the provider used to authenticate.
   *
   * <p>LDAP authentication goes through the form client, so it is detected from the profile. Other
   * providers are resolved with {@link ClientTitles}, from the client if found, from its name
   * otherwise. An unknown client name is returned as is.
   *
   * @param clientName the technical client name
   * @param profile the authenticated profile, may be {@code null}
   * @return the provider name, or {@code null} if the client name is unknown
   */
  public String getProviderTitle(String clientName, CommonProfile profile) {
    if (profile instanceof LdapProfile) {
      return LDAP_TITLE;
    }
    if (StringUtils.isBlank(clientName)) {
      return null;
    }

    final Client client = findClient(clientName);
    final String title = client != null ? ClientTitles.of(client) : ClientTitles.of(clientName);

    return title != null ? title : clientName;
  }

  private Client findClient(String clientName) {
    return clientListService.get().get().stream()
        .filter(client -> clientName.equals(client.getName()))
        .findFirst()
        .orElse(null);
  }

  private String getMfaMethod(CommonProfile profile) {
    return profile != null
        ? (String) profile.getAuthenticationAttribute(MfaAuthenticator.MFA_AUTHENTICATED_METHOD)
        : null;
  }

  private boolean accept(String clientName) {
    return !(clientName != null
        && clientListService.get().getDirectClientNames().contains(clientName));
  }

  private void putDetail(Map<String, Object> details, String key, String value) {
    if (StringUtils.notBlank(value)) {
      details.put(key, value);
    }
  }

  private String toJson(Map<String, Object> details) throws JsonProcessingException {
    return details.isEmpty() ? null : objectMapper.writeValueAsString(details);
  }
}
