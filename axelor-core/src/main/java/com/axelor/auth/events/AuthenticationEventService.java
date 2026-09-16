/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.events;

import com.axelor.auth.UserAgentParser;
import com.axelor.auth.UserAuthenticationEvent;
import com.axelor.auth.UserSession;
import com.axelor.auth.db.AuthenticationEvent;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.db.Query;
import com.axelor.i18n.I18n;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationEventService {

  private static final Logger log = LoggerFactory.getLogger(AuthenticationEventService.class);

  @Inject private UserAgentParser uaParser;
  @Inject private ObjectMapper objectMapper;

  private static final String SIGNED_IN_WITH = /*$$(*/ "%s authentication" /*)*/;
  private static final String SIGNED_IN_WITH_USING = /*$$(*/ "%s authentication using %s" /*)*/;
  private static final String MFA_TOTP = /*$$(*/ "2FA authenticator app" /*)*/;
  private static final String MFA_EMAIL = /*$$(*/ "2FA email code" /*)*/;
  private static final String MFA_RECOVERY = /*$$(*/ "2FA recovery code" /*)*/;

  /**
   * Loads a page of the authentication events of the given user, most recent first.
   *
   * <p>One more event than the page size is fetched, to tell whether there is a next page.
   */
  public List<UserAuthenticationEvent> loadAuthenticationEvents(
      User user, int offset, int pageSize) {
    return Query.of(AuthenticationEvent.class)
        .filter("self.user = :user")
        .bind("user", user)
        .order("-createdOn")
        .order("-id")
        .fetch(pageSize + 1, offset)
        .stream()
        .map(this::toUserAuthenticationEvent)
        .toList();
  }

  private UserAuthenticationEvent toUserAuthenticationEvent(AuthenticationEvent event) {
    final Map<String, Object> details = parseDetails(event.getDetails());
    final LocalDateTime createdOn = event.getCreatedOn();

    UserAgentParser.UserAgentInfo userAgentInfo =
        uaParser.parse((String) details.get(AuthenticationEventRecorder.DETAIL_USER_AGENT));

    return new UserAuthenticationEvent(
        getSignInMessage(
            event.getProvider(),
            (String) details.get(AuthenticationEventRecorder.DETAIL_MFA_METHOD)),
        event.getStatus() != null ? event.getStatus().name() : null,
        createdOn != null ? createdOn.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : 0,
        UserSession.Device.of(event.getIpAddress(), userAgentInfo));
  }

  private String getSignInMessage(String provider, String mfaMethod) {
    final String mfa = getMfaMethodTitle(mfaMethod);

    if (StringUtils.isBlank(provider)) {
      provider = "";
    }
    return mfa == null
        ? I18n.get(SIGNED_IN_WITH).formatted(provider)
        : I18n.get(SIGNED_IN_WITH_USING).formatted(provider, mfa);
  }

  private String getMfaMethodTitle(String mfaMethod) {
    if (StringUtils.isBlank(mfaMethod)) {
      return null;
    }
    return switch (mfaMethod.toUpperCase()) {
      case "TOTP" -> I18n.get(MFA_TOTP);
      case "EMAIL" -> I18n.get(MFA_EMAIL);
      case "RECOVERY" -> I18n.get(MFA_RECOVERY);
      default -> mfaMethod;
    };
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> parseDetails(String details) {
    if (StringUtils.isBlank(details)) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(details, Map.class);
    } catch (JsonProcessingException e) {
      log.warn("Unable to parse authentication event details: {}", details, e);
      return Map.of();
    }
  }
}
