/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import static com.axelor.auth.pac4j.AxelorProfileManager.AVAILABLE_MFA_METHODS;
import static com.axelor.auth.pac4j.AxelorProfileManager.PENDING_USER_NAME;

import com.axelor.app.AppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.MFAService;
import com.axelor.auth.db.MFAMethod;
import com.axelor.auth.db.User;
import com.axelor.auth.pac4j.local.AxelorFormClient;
import com.axelor.common.StringUtils;
import com.axelor.common.UriBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.finder.DefaultCallbackClientFinder;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.FrameworkParameters;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.engine.DefaultCallbackLogic;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.OkAction;
import org.pac4j.core.exception.http.WithLocationAction;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.util.HttpActionHelper;
import org.pac4j.core.util.Pac4jConstants;
import org.pac4j.jee.context.JEEFrameworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AxelorCallbackLogic extends DefaultCallbackLogic {

  private final ErrorHandler errorHandler;
  private final AxelorCsrfMatcher csrfMatcher;
  private final AuthPac4jInfo pac4jInfo;
  private final MFAService mfaService;
  private final AxelorUrlResolver urlResolver;

  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Inject
  public AxelorCallbackLogic(
      ErrorHandler errorHandler,
      AxelorCsrfMatcher csrfMatcher,
      DefaultCallbackClientFinder clientFinder,
      AuthPac4jInfo pac4jInfo,
      MFAService mfaService,
      AxelorUrlResolver urlResolver) {
    this.errorHandler = errorHandler;
    this.csrfMatcher = csrfMatcher;
    this.pac4jInfo = pac4jInfo;
    this.mfaService = mfaService;
    this.urlResolver = urlResolver;
    setClientFinder(clientFinder);
  }

  @Override
  public Object perform(
      Config config,
      String inputDefaultUrl,
      Boolean inputRenewSession,
      String defaultClient,
      FrameworkParameters parameters) {

    final var jeeParameters = (JEEFrameworkParameters) parameters;

    try {
      jeeParameters.getRequest().setCharacterEncoding("UTF-8");
    } catch (UnsupportedEncodingException e) {
      final var context = config.getWebContextFactory().newContext(parameters);
      return handleException(e, config.getHttpActionAdapter(), context);
    }

    return super.perform(
        config, AppSettings.get().getBaseURL(), inputRenewSession, defaultClient, parameters);
  }

  /**
   * Redefined to account for failed clients
   *
   * @see com.axelor.auth.pac4j.AxelorCallbackClientFinder
   */
  @Override
  protected void renewSession(CallContext ctx, Config config) {
    final var context = ctx.webContext();
    final var sessionStore = ctx.sessionStore();

    final var optOldSessionId = sessionStore.getSessionId(context, true);
    if (optOldSessionId.isEmpty()) {
      logger.error(
          "No old session identifier retrieved although the session creation has been requested");
    } else {
      final var oldSessionId = optOldSessionId.get();
      final var renewed = sessionStore.renewSession(context);
      if (renewed) {
        final var optNewSessionId = sessionStore.getSessionId(context, true);
        if (optNewSessionId.isEmpty()) {
          logger.error(
              "No new session identifier retrieved although the session creation has been"
                  + " requested");
        } else {
          final var newSessionId = optNewSessionId.get();
          logger.debug("Renewing session: {} -> {}", oldSessionId, newSessionId);
          final var clients = config.getClients();
          if (clients != null) {
            final var clientList = clients.getClients();
            for (final var client : clientList) {
              final var baseClient = (BaseClient) client;

              // Don't fail because of any unavailable clients.
              if (baseClient.isInitialized()) {
                try {
                  baseClient.notifySessionRenewal(ctx, oldSessionId);
                } catch (Exception e) {
                  logger.error(e.getMessage(), e);
                }
              }
            }
          }
        }
      } else {
        logger.error("Unable to renew the session. The session store may not support this feature");
      }
    }
  }

  @Override
  protected HttpAction redirectToOriginallyRequestedUrl(CallContext ctx, String defaultUrl) {
    // Add CSRF token cookie and header
    csrfMatcher.addResponseCookieAndHeader(ctx);

    final var context = ctx.webContext();
    final var sessionStore = ctx.sessionStore();

    final var pendingUsername = sessionStore.get(context, PENDING_USER_NAME).map(Object::toString);

    if (pendingUsername.isPresent()) {
      @SuppressWarnings("unchecked")
      List<MFAMethod> methods =
          sessionStore
              .get(context, AVAILABLE_MFA_METHODS)
              .filter(List.class::isInstance)
              .map(o -> (List<MFAMethod>) o)
              .orElse(Collections.emptyList());
      return mfaAction(pendingUsername.get(), methods, context);
    }

    // If XHR, return status code only
    if (AuthPac4jInfo.isXHR(ctx)) {
      return new OkAction("{}");
    }

    final String requestedUrl =
        sessionStore
            .get(context, Pac4jConstants.REQUESTED_URL)
            .filter(WithLocationAction.class::isInstance)
            .map(action -> ((WithLocationAction) action).getLocation())
            .orElse("");

    String redirectUrl = defaultUrl;
    if (StringUtils.notBlank(requestedUrl)) {
      sessionStore.set(context, Pac4jConstants.REQUESTED_URL, null);
      redirectUrl = requestedUrl;
    }

    final String hashLocation =
        context
            .getRequestParameter(AxelorSecurityLogic.HASH_LOCATION_PARAMETER)
            .or(
                () ->
                    sessionStore
                        .get(context, AxelorSecurityLogic.HASH_LOCATION_PARAMETER)
                        .map(String::valueOf))
            .orElse("");
    if (StringUtils.notBlank(hashLocation)) {
      redirectUrl = UriBuilder.from(redirectUrl).setFragment(hashLocation).toUri().toString();
    }

    logger.debug("redirectUrl: {}", redirectUrl);
    return HttpActionHelper.buildRedirectUrlAction(context, redirectUrl);
  }

  protected HttpAction mfaAction(String username, List<MFAMethod> methods, WebContext context) {
    try {
      context.setResponseContentType(HttpConstants.APPLICATION_JSON + "; charset=utf-8");
      final Map<String, Object> state = new HashMap<>();
      state.putAll(Map.of("methods", methods, "username", username));

      if (methods.stream().anyMatch(method -> method == MFAMethod.EMAIL)) {
        processEmailMethod(state, username, methods.get(0) == MFAMethod.EMAIL);
      }

      final Map<String, Object> responseData =
          Map.of("route", Map.of("path", "/mfa", "state", state));
      final HttpAction action;

      if (AuthPac4jInfo.isXHR(context)) {
        final String content = new ObjectMapper().writeValueAsString(responseData);
        action = HttpActionHelper.buildFormPostContentAction(context, content);
      } else {
        // Query params into fragment for HashRouter
        var fragmentBuilder = jakarta.ws.rs.core.UriBuilder.fromPath("/mfa");
        addQueryParams(fragmentBuilder, state);
        var fragment = fragmentBuilder.build().toString();
        var uriBuilder =
            jakarta.ws.rs.core.UriBuilder.fromPath(
                    urlResolver.compute(AxelorFormClient.LOGIN_URL, context))
                .fragment(fragment);
        String redirectionUrl = uriBuilder.build().toString();
        action = HttpActionHelper.buildRedirectUrlAction(context, redirectionUrl);
      }

      return action;
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to process MFA response data", e);
    }
  }

  // Allows repeated params
  private void addQueryParams(jakarta.ws.rs.core.UriBuilder uriBuilder, Map<String, Object> state) {
    state.forEach(
        (key, value) -> {
          if (value instanceof Collection<?> collection) {
            collection.forEach(item -> uriBuilder.queryParam(key, item.toString()));
          } else if (value != null) {
            uriBuilder.queryParam(key, value.toString());
          }
        });
  }

  private void processEmailMethod(Map<String, Object> state, String username, boolean isDefault) {
    User user = AuthUtils.getUser(username);
    LocalDateTime emailRetryAfter = mfaService.getEmailRetryAfter(user);

    if (emailRetryAfter == null && isDefault) {
      try {
        emailRetryAfter = mfaService.sendEmailCode(user);
      } catch (Exception e) {
        logger.error("Failed to send MFA email for user %s".formatted(user.getCode()), e);
      }
    }

    if (emailRetryAfter != null) {
      state.put("emailRetryAfter", format(emailRetryAfter));
    }
  }

  private String format(LocalDateTime localDateTime) {
    return localDateTime
        .atZone(ZoneId.systemDefault())
        .withZoneSameInstant(ZoneOffset.UTC)
        .toString();
  }

  @Override
  protected Object handleException(
      Exception e, HttpActionAdapter httpActionAdapter, WebContext context) {
    return errorHandler.handleException(e, httpActionAdapter, context);
  }
}
