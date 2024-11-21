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
package com.axelor.auth.pac4j.local;

import static org.pac4j.core.util.CommonHelper.assertNotNull;

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
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.extractor.FormExtractor;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.http.HttpAction;
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
      setAuthenticatorIfUndefined(authPac4jInfo.getAuthenticator());
      setCredentialsExtractor(Beans.get(FormExtractor.class));
      setUrlResolver(new AxelorUrlResolver());
    }

    super.internalInit(forceReinit);
  }

  @Override
  protected Optional<Credentials> internalValidateCredentials(
      final CallContext ctx, final Credentials credentials) {
    assertNotNull("authenticator", getAuthenticator());

    final var username = ((UsernamePasswordCredentials) credentials).getUsername();
    try {
      return getAuthenticator().validate(ctx, credentials);
    } catch (ChangePasswordException e) {
      throw handleChangePassword(ctx, username, e);
    } catch (CredentialsException e) {
      throw handleInvalidCredentials(ctx, username, e);
    }
  }

  @Override
  protected String computeErrorMessage(Exception e) {
    return e.getMessage();
  }

  @Override
  protected HttpAction handleInvalidCredentials(
      CallContext ctx, String username, String message, String errorMessage) {
    return handleInvalidCredentials(ctx, username, new CredentialsException(errorMessage));
  }

  private HttpAction handleChangePassword(
      CallContext ctx, String username, ChangePasswordException exception) {
    final var context = ctx.webContext();
    final var sessionStore = ctx.sessionStore();
    final var errorMessage = computeErrorMessage(exception);

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

    throw handleInvalidCredentials(ctx, username, exception);
  }

  private HttpAction handleInvalidCredentials(
      CallContext ctx, String username, CredentialsException exception) {
    final var context = ctx.webContext();
    final var errorMessage = computeErrorMessage(exception);

    logger.error("Authentication failed for user \"{}\": {}", username, errorMessage);
    credentialsHandler.handleInvalidCredentials(this, username, exception);

    // it's an AJAX request -> unauthorized (instead of a redirection)
    if (getAjaxRequestResolver().isAjax(ctx)) {
      logger.info("AJAX request detected -> returning 401");
      return HttpActionHelper.buildUnauthenticatedAction(context);
    } else {
      // Make sure to compute an absolute redirection url
      String redirectionUrl =
          UriBuilder.from(urlResolver.compute(getLoginUrl(), context))
              .addQueryParam(getUsernameParameter(), username)
              .addQueryParam(ERROR_PARAMETER, AxelorAuthenticator.INCORRECT_CREDENTIALS)
              .toUri()
              .toString();
      logger.debug("redirectionUrl: {}", redirectionUrl);
      return HttpActionHelper.buildRedirectUrlAction(context, redirectionUrl);
    }
  }
}
