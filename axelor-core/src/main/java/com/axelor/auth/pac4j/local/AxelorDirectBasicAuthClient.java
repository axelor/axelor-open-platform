/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j.local;

import com.axelor.auth.pac4j.AuthPac4jInfo;
import com.axelor.inject.Beans;
import java.util.Optional;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.exception.BadCredentialsException;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.http.client.direct.DirectBasicAuthClient;

public class AxelorDirectBasicAuthClient extends DirectBasicAuthClient {

  private CredentialsHandler credentialsHandler;

  @Override
  protected void internalInit(boolean forceReinit) {
    if (credentialsHandler == null || forceReinit) {
      credentialsHandler = Beans.get(CredentialsHandler.class);
      setAuthenticatorIfUndefined(Beans.get(AuthPac4jInfo.class).getAuthenticator());
    }
    super.internalInit(forceReinit);
  }

  @Override
  public Optional<Credentials> getCredentials(CallContext ctx) {
    final var context = ctx.webContext();

    if (context.getRequestHeader(HttpConstants.AUTHORIZATION_HEADER).isEmpty()) {
      return Optional.empty();
    }

    return super.getCredentials(ctx);
  }

  @Override
  protected Optional<Credentials> internalValidateCredentials(
      CallContext ctx, Credentials credentials) {
    var validatedCredentials = super.internalValidateCredentials(ctx, credentials);

    if (validatedCredentials.isEmpty()) {
      Optional<Credentials> credentialsOpt = Optional.empty();
      CredentialsException error;

      try {
        credentialsOpt = getCredentialsExtractor().extract(ctx);
        error = new BadCredentialsException(AxelorAuthenticator.INCORRECT_CREDENTIALS);
      } catch (CredentialsException e) {
        error = e;
      }

      final String username =
          credentialsOpt
              .filter(UsernamePasswordCredentials.class::isInstance)
              .map(UsernamePasswordCredentials.class::cast)
              .map(UsernamePasswordCredentials::getUsername)
              .orElse(null);
      credentialsHandler.handleInvalidCredentials(this, username, error);
    } else {
      var context = ctx.webContext();
      context.setRequestAttribute(DefaultSubjectContext.SESSION_CREATION_ENABLED, Boolean.FALSE);
    }

    return validatedCredentials;
  }
}
