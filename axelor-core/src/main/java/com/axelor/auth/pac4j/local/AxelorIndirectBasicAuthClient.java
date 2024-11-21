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
