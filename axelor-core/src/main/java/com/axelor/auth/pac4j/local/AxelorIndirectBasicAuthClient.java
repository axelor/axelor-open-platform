package com.axelor.auth.pac4j.local;

import com.axelor.auth.pac4j.AuthPac4jInfo;
import com.axelor.inject.Beans;
import java.util.Optional;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.exception.BadCredentialsException;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.http.client.indirect.IndirectBasicAuthClient;

public class AxelorIndirectBasicAuthClient extends IndirectBasicAuthClient {

  private CredentialsHandler credentialsHandler;

  @Override
  protected void clientInit() {
    defaultAuthenticator(Beans.get(AuthPac4jInfo.class).getAuthenticator());
    credentialsHandler = Beans.get(CredentialsHandler.class);

    super.clientInit();
  }

  @Override
  protected Optional<UsernamePasswordCredentials> retrieveCredentials(WebContext context) {
    if (context.getRequestHeader(HttpConstants.AUTHORIZATION_HEADER).isEmpty()) {
      return Optional.empty();
    }

    final Optional<UsernamePasswordCredentials> credentials = super.retrieveCredentials(context);

    if (credentials.isEmpty()) {
      Optional<UsernamePasswordCredentials> credentialsOpt = Optional.empty();
      CredentialsException error;
      try {
        credentialsOpt = getCredentialsExtractor().extract(context);
        error = new BadCredentialsException(AxelorAuthenticator.INCORRECT_CREDENTIALS);
      } catch (CredentialsException e) {
        error = e;
      }

      final String username =
          credentialsOpt.map(UsernamePasswordCredentials::getUsername).orElse(null);
      credentialsHandler.handleInvalidCredentials(this, username, error);
    }

    return credentials;
  }
}
