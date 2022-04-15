package com.axelor.auth.pac4j.local;

import com.axelor.auth.pac4j.AxelorCallbackClientFinder;
import java.util.List;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.WebContext;

/**
 * Checks for basic auth request header and finds basic auth client when appropriate. This avoids
 * having to specify request parameter "client_name" when using IndirectBasicAuth.
 */
public class BasicAuthCallbackClientFinder extends AxelorCallbackClientFinder {

  private final String clientName;

  public BasicAuthCallbackClientFinder(String clientName) {
    this.clientName = clientName;
  }

  @Override
  public List<Client> find(Clients clients, WebContext context, String clientNames) {

    if (context
        .getRequestHeader(HttpConstants.AUTHORIZATION_HEADER)
        .map(header -> header.startsWith(HttpConstants.BASIC_HEADER_PREFIX))
        .isPresent()) {
      clientNames = clientName;
    }

    return super.find(clients, context, clientNames);
  }
}
