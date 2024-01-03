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
package com.axelor.auth.pac4j.local;

import com.axelor.auth.AuthService;
import com.axelor.auth.pac4j.AuthPac4jInfo;
import com.axelor.auth.pac4j.AxelorUrlResolver;
import com.axelor.common.StringUtils;
import com.axelor.common.UriBuilder;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Map;
import java.util.Optional;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.extractor.FormExtractor;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.http.ajax.AjaxRequestResolver;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.core.util.HttpActionHelper;
import org.pac4j.http.client.indirect.FormClient;

public class AxelorFormClient extends FormClient {

  public static final String LOGIN_URL = "/index.html";

  private CredentialsHandler credentialsHandler;

  @Override
  protected void internalInit(boolean forceReinit) {
    if (credentialsHandler == null || forceReinit) {
      credentialsHandler = Beans.get(CredentialsHandler.class);
      final AuthPac4jInfo authPac4jInfo = Beans.get(AuthPac4jInfo.class);
      setLoginUrl(LOGIN_URL);
      defaultAuthenticator(authPac4jInfo.getAuthenticator());
      setCredentialsExtractor(Beans.get(FormExtractor.class));
      setAjaxRequestResolver(Beans.get(AjaxRequestResolver.class));
      setUrlResolver(new AxelorUrlResolver());
    }

    super.internalInit(forceReinit);
  }

  @Override
  protected Optional<Credentials> retrieveCredentials(
      WebContext context, SessionStore sessionStore) {
    CommonHelper.assertNotNull("credentialsExtractor", getCredentialsExtractor());
    CommonHelper.assertNotNull("authenticator", getAuthenticator());

    String username = context.getRequestParameter(getUsernameParameter()).orElse(null);
    final Optional<Credentials> credentials;
    try {
      // retrieve credentials
      credentials = getCredentialsExtractor().extract(context, sessionStore);
      logger.debug("usernamePasswordCredentials: {}", credentials);
      if (credentials.isEmpty()) {
        throw handleInvalidCredentials(
            context,
            sessionStore,
            username,
            "Username and password cannot be blank -> return to the form with error",
            MISSING_FIELD_ERROR);
      }
      final Credentials cred = credentials.get();
      // username in AJAX request
      if (StringUtils.isBlank(username) && cred instanceof UsernamePasswordCredentials) {
        username = ((UsernamePasswordCredentials) cred).getUsername();
      }
      // validate credentials
      getAuthenticator().validate(cred, context, sessionStore);
    } catch (final CredentialsException e) {
      throw handleInvalidCredentials(
          context,
          sessionStore,
          username,
          "Credentials validation fails -> return to the form with error",
          e);
    }

    return credentials;
  }

  @Override
  protected String computeErrorMessage(Exception e) {
    return e.getMessage();
  }

  @Override
  protected HttpAction handleInvalidCredentials(
      WebContext context,
      SessionStore sessionStore,
      String username,
      String message,
      String errorMessage) {
    return handleInvalidCredentials(
        context, sessionStore, username, message, new CredentialsException(errorMessage));
  }

  protected HttpAction handleInvalidCredentials(
      WebContext context,
      SessionStore sessionStore,
      String username,
      String message,
      CredentialsException exception) {

    final String errorMessage = computeErrorMessage(exception);

    if (exception instanceof ChangePasswordException) {
      context
          .getRequestParameter("tenantId")
          .ifPresent(tenantId -> sessionStore.set(context, "tenantId", tenantId));

      final AuthService authService = AuthService.getInstance();

      try {
        context.setResponseContentType(HttpConstants.APPLICATION_JSON + "; charset=utf-8");

        final Builder<String, String> stateBuilder =
            new ImmutableMap.Builder<String, String>()
                .put("passwordPattern", authService.getPasswordPattern())
                .put("passwordPatternTitle", authService.getPasswordPatternTitle());
        if (StringUtils.notBlank(errorMessage)) {
          stateBuilder.put(ERROR_PARAMETER, I18n.get(errorMessage));
        }
        final Map<String, String> state = stateBuilder.build();

        final String content =
            (new ObjectMapper())
                .writeValueAsString(
                    Map.of("route", Map.of("path", "/change-password", "state", state)));
        return HttpActionHelper.buildFormPostContentAction(context, content);
      } catch (JsonProcessingException e) {
        logger.error(e.getMessage(), e);
      }
    }

    logger.error("Authentication failed for user \"{}\": {}", username, errorMessage);
    credentialsHandler.handleInvalidCredentials(this, username, exception);
    return handleInvalidCredentialsInternal(
        context, sessionStore, username, message, AxelorAuthenticator.INCORRECT_CREDENTIALS);
  }

  // Make sure to compute an absolute redirection url
  protected HttpAction handleInvalidCredentialsInternal(
      final WebContext context,
      final SessionStore sessionStore,
      final String username,
      String message,
      String errorMessage) {
    // it's an AJAX request -> unauthorized (instead of a redirection)
    if (getAjaxRequestResolver().isAjax(context, sessionStore)) {
      logger.info("AJAX request detected -> returning 401");
      return HttpActionHelper.buildUnauthenticatedAction(context);
    } else {
      String redirectionUrl =
          UriBuilder.from(urlResolver.compute(getLoginUrl(), context))
              .addQueryParam(getUsernameParameter(), username)
              .addQueryParam(ERROR_PARAMETER, errorMessage)
              .toUri()
              .toString();
      logger.debug("redirectionUrl: {}", redirectionUrl);
      return HttpActionHelper.buildRedirectUrlAction(context, redirectionUrl);
    }
  }
}
