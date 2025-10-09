/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j.local;

import com.axelor.auth.pac4j.AxelorUrlResolver;
import com.axelor.inject.Beans;
import java.util.Optional;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.extractor.FormExtractor;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.util.HttpActionHelper;

public class MfaClient extends IndirectClient {

  private CredentialsHandler credentialsHandler;

  @Override
  protected void internalInit(boolean forceReinit) {
    if (credentialsHandler == null || forceReinit) {
      credentialsHandler = Beans.get(CredentialsHandler.class);
      setAuthenticator(Beans.get(MfaAuthenticator.class));
      setCredentialsExtractor(Beans.get(FormExtractor.class));
      setUrlResolver(Beans.get(AxelorUrlResolver.class));
    }
  }

  @Override
  protected Optional<Credentials> internalValidateCredentials(
      final CallContext ctx, final Credentials credentials) {

    final var username = ((UsernamePasswordCredentials) credentials).getUsername();
    try {
      return getAuthenticator().validate(ctx, credentials);
    } catch (CredentialsException e) {
      throw handleInvalidCredentials(ctx, username, e);
    }
  }

  private HttpAction handleInvalidCredentials(
      CallContext ctx, String username, CredentialsException exception) {
    logger.error("MFA failed for user \"{}\": {}", username, exception.getMessage());
    credentialsHandler.handleInvalidCredentials(this, username, exception);
    return HttpActionHelper.buildUnauthenticatedAction(ctx.webContext());
  }
}
