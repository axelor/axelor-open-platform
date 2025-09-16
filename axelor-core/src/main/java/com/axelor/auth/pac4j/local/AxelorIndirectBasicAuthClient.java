/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j.local;

import com.axelor.auth.pac4j.AuthPac4jInfo;
import com.axelor.inject.Beans;
import java.util.Optional;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.exception.BadCredentialsException;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.http.client.indirect.IndirectBasicAuthClient;

public class AxelorIndirectBasicAuthClient extends IndirectBasicAuthClient {

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

    final Optional<Credentials> credentials = super.getCredentials(ctx);

    if (credentials.isEmpty()) {
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
    }

    return credentials;
  }
}
