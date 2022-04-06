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
package com.axelor.auth.pac4j.local;

import com.axelor.auth.pac4j.AuthPac4jInfo;
import com.axelor.common.StringUtils;
import com.axelor.inject.Beans;
import java.util.Optional;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.extractor.FormExtractor;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.RedirectionActionHelper;
import org.pac4j.core.http.ajax.AjaxRequestResolver;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.http.client.indirect.FormClient;

public class AxelorFormClient extends FormClient {

  private CredentialsHandler credentialsHandler;

  @Override
  protected void clientInit() {
    final AuthPac4jInfo authPac4jInfo = Beans.get(AuthPac4jInfo.class);
    setLoginUrl(authPac4jInfo.getBaseUrl() + "/login.jsp");
    defaultAuthenticator(authPac4jInfo.getAuthenticator());
    setCredentialsExtractor(Beans.get(FormExtractor.class));
    setAjaxRequestResolver(Beans.get(AjaxRequestResolver.class));
    credentialsHandler = Beans.get(CredentialsHandler.class);

    super.clientInit();
  }

  @Override
  protected Optional<UsernamePasswordCredentials> retrieveCredentials(WebContext context) {
    CommonHelper.assertNotNull("credentialsExtractor", getCredentialsExtractor());
    CommonHelper.assertNotNull("authenticator", getAuthenticator());

    String username = context.getRequestParameter(getUsernameParameter()).orElse("");
    final Optional<UsernamePasswordCredentials> credentials;
    try {
      // retrieve credentials
      credentials = getCredentialsExtractor().extract(context);
      logger.debug("usernamePasswordCredentials: {}", credentials);
      if (credentials.isEmpty()) {
        throw handleInvalidCredentials(
            context,
            username,
            "Username and password cannot be blank -> return to the form with error",
            MISSING_FIELD_ERROR);
      }
      final UsernamePasswordCredentials cred = credentials.get();
      // username in AJAX request
      if (StringUtils.isBlank(username)) {
        username = cred.getUsername();
      }
      // validate credentials
      getAuthenticator().validate(cred, context);
    } catch (final CredentialsException e) {
      throw handleInvalidCredentials(
          context, username, "Credentials validation fails -> return to the form with error", e);
    }

    return credentials;
  }

  @Override
  protected String computeErrorMessage(Exception e) {
    return e.getMessage();
  }

  @Override
  protected HttpAction handleInvalidCredentials(
      WebContext context, String username, String message, String errorMessage) {
    return handleInvalidCredentials(
        context, username, message, new CredentialsException(errorMessage));
  }

  protected HttpAction handleInvalidCredentials(
      WebContext context, String username, String message, CredentialsException e) {

    final String errorMessage = computeErrorMessage(e);

    if (e instanceof ChangePasswordException
        || context.getRequestParameter(AxelorAuthenticator.NEW_PASSWORD_PARAMETER).isPresent()) {

      context
          .getRequestParameter("tenantId")
          .ifPresent(
              tenantId -> {
                @SuppressWarnings("unchecked")
                final SessionStore<WebContext> sessionStore = context.getSessionStore();
                sessionStore.set(context, "tenantId", tenantId);
              });

      String redirectUrl =
          CommonHelper.addParameter("change-password.jsp", getUsernameParameter(), username);
      redirectUrl = CommonHelper.addParameter(redirectUrl, ERROR_PARAMETER, errorMessage);
      return RedirectionActionHelper.buildRedirectUrlAction(context, redirectUrl);
    }

    logger.error("Authentication failed for user \"{}\": {}", username, errorMessage);
    credentialsHandler.handleInvalidCredentials(this, username, e);
    return super.handleInvalidCredentials(
        context, username, message, AxelorAuthenticator.INCORRECT_CREDENTIALS);
  }
}
