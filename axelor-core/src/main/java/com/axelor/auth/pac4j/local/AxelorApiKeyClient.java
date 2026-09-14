/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j.local;

import com.google.inject.Inject;
import java.util.Optional;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.exception.BadCredentialsException;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.http.client.direct.HeaderClient;

public class AxelorApiKeyClient extends HeaderClient implements StatelessClient {

  private static final String API_KEY = "API-KEY";

  private CredentialsHandler credentialsHandler;

  @Inject
  public AxelorApiKeyClient(
      AxelorApiKeyAuthenticator axelorApiKeyAuthenticator, CredentialsHandler credentialsHandler) {
    super(API_KEY, axelorApiKeyAuthenticator);
    this.credentialsHandler = credentialsHandler;
  }

  @Override
  public boolean hasCredentials(WebContext context) {
    return context.getRequestHeader(getHeaderName()).isPresent();
  }

  @Override
  public Optional<Credentials> getCredentials(CallContext ctx) {
    final var context = ctx.webContext();

    if (!hasCredentials(context)) {
      return Optional.empty();
    }

    disableSessionCreation(context);

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
        error = new BadCredentialsException(AxelorApiKeyAuthenticator.INVALID_API_KEY);
      } catch (CredentialsException e) {
        error = e;
      }

      final String token =
          credentialsOpt
              .filter(TokenCredentials.class::isInstance)
              .map(TokenCredentials.class::cast)
              .map(TokenCredentials::getToken)
              .orElse(null);
      credentialsHandler.handleInvalidCredentials(this, token, error);
    }

    return validatedCredentials;
  }
}
