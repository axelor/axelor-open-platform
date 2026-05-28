/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.client.finder.DefaultCallbackClientFinder;
import org.pac4j.core.context.WebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Callback client finder that doesn’t fail when some indirect clients are unavailable. */
public class AxelorCallbackClientFinder extends DefaultCallbackClientFinder {

  protected final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public List<Client> find(
      final Clients clients, final WebContext context, final String clientNames) {

    final List<Client> result = new ArrayList<>();
    final List<Client> indirectClients = new ArrayList<>();

    clients.findAllClients().stream()
        .filter(IndirectClient.class::isInstance)
        .map(IndirectClient.class::cast)
        .forEach(
            client -> {
              try {
                client.init(!isInitialized(client));
              } catch (Exception e) {
                logger.error("{}: {}", client.getName(), e.getMessage());
              }
              if (isInitialized(client)) {
                indirectClients.add(client);
                if (client.getCallbackUrlResolver().matches(client.getName(), context)) {
                  result.add(client);
                }
              }
            });

    logger.debug("result: {}", result.stream().map(Client::getName).collect(Collectors.toList()));

    // fallback: no client found and we have a default client, use it
    if (result.isEmpty() && StringUtils.isNotBlank(clientNames)) {
      clients
          .findClient(clientNames)
          .filter(this::isInitialized)
          .ifPresent(
              client -> {
                logger.debug("Defaulting to the configured client: {}", client);
                result.add(client);
              });
    }

    // fallback: no client found and we only have one indirect client, use it
    if (result.isEmpty() && indirectClients.size() == 1) {
      logger.debug("Defaulting to the only client: {}", indirectClients.getFirst());
      result.addAll(indirectClients);
    }

    return result;
  }

  private boolean isInitialized(Client client) {
    if (client instanceof BaseClient baseClient) {
      return baseClient.getCredentialsExtractor() != null && baseClient.getAuthenticator() != null;
    }
    return true;
  }
}
