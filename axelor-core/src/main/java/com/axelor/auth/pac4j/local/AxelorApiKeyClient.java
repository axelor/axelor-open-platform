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
import org.pac4j.core.exception.BadCredentialsException;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.http.client.direct.HeaderClient;

/**
 * Direct client for API key-based authentication.
 *
 * <p>Reads the API key from the {@code API-KEY} request header and delegates its validation to
 * {@link AxelorApiKeyAuthenticator}.
 *
 * <p>Being a {@link StatelessClient}, it authenticates each request on its own and never lets a
 * session be created.
 *
 * <p>Rejected API keys never reach a Shiro realm, so they are reported to the {@link
 * CredentialsHandler} for the failed login to be tracked.
 */
public class AxelorApiKeyClient extends HeaderClient implements StatelessClient {

  private static final String API_KEY = "API-KEY";

  private CredentialsHandler credentialsHandler;

  @Inject
  public AxelorApiKeyClient(
      AxelorApiKeyAuthenticator axelorApiKeyAuthenticator, CredentialsHandler credentialsHandler) {
    super(API_KEY, axelorApiKeyAuthenticator);
    this.credentialsHandler = credentialsHandler;
  }

  /**
   * Whether the request carries an API key.
   *
   * @param context the web context
   * @return {@code true} if the {@code API-KEY} header is present
   */
  @Override
  public boolean hasCredentials(WebContext context) {
    return context.getRequestHeader(getHeaderName()).isPresent();
  }

  /**
   * Extracts the API key credentials from the request.
   *
   * <p>Session creation is disabled as soon as an API key is found, so that authenticating with an
   * API key never creates a session.
   *
   * @param ctx the call context
   * @return the extracted credentials, or empty if the request carries no API key
   */
  @Override
  public Optional<Credentials> getCredentials(CallContext ctx) {
    final var context = ctx.webContext();

    if (!hasCredentials(context)) {
      return Optional.empty();
    }

    disableSessionCreation(context);

    return super.getCredentials(ctx);
  }

  /**
   * Validates the extracted credentials against the authenticator.
   *
   * <p>The authenticator rejects invalid credentials before any Shiro login is attempted, so such
   * failures are reported to the {@link CredentialsHandler}, along with the submitted API key when
   * it could be extracted.
   *
   * @param ctx the call context
   * @param credentials the credentials to validate
   * @return the validated credentials, or empty if they were rejected
   */
  @Override
  protected Optional<Credentials> internalValidateCredentials(
      CallContext ctx, Credentials credentials) {
    var validatedCredentials = super.internalValidateCredentials(ctx, credentials);

    if (validatedCredentials.isEmpty()) {
      CredentialsException error =
          new BadCredentialsException(AxelorApiKeyAuthenticator.INVALID_API_KEY);
      credentialsHandler.handleInvalidCredentials(this, null, error);
    }

    return validatedCredentials;
  }
}
