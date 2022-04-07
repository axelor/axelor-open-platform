package com.axelor.auth.pac4j.local;

import java.util.List;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.finder.DefaultCallbackClientFinder;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;

/**
 * Checks for basic auth request header and finds basic auth client when appropriate. This avoids
 * having to specify request parameter "client_name" when using IndirectBasicAuth.
 */
public class BasicAuthCallbackClientFinder extends DefaultCallbackClientFinder {

  private final String clientName;

  public BasicAuthCallbackClientFinder(String clientName) {
    this.clientName = clientName;
  }

  @Override
  public List<Client<? extends Credentials>> find(
      Clients clients, WebContext context, String clientNames) {

    if (context
        .getRequestHeader(HttpConstants.AUTHORIZATION_HEADER)
        .map(header -> header.startsWith(HttpConstants.BASIC_HEADER_PREFIX))
        .isPresent()) {
      clientNames = clientName;
    }

    return super.find(clients, context, clientNames);
  }
}
