package com.axelor.auth.pac4j.local;

import com.axelor.auth.pac4j.AuthPac4jInfo;
import com.axelor.inject.Beans;
import java.util.Optional;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
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
      defaultAuthenticator(Beans.get(AuthPac4jInfo.class).getAuthenticator());
    }
    super.internalInit(forceReinit);
  }

  @Override
  protected Optional<Credentials> retrieveCredentials(
      WebContext context, SessionStore sessionStore) {
    if (context.getRequestHeader(HttpConstants.AUTHORIZATION_HEADER).isEmpty()) {
      return Optional.empty();
    }

    final Optional<Credentials> credentials = super.retrieveCredentials(context, sessionStore);

    if (credentials.isEmpty()) {
      Optional<Credentials> credentialsOpt = Optional.empty();
      CredentialsException error;
      try {
        credentialsOpt = getCredentialsExtractor().extract(context, sessionStore);
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
