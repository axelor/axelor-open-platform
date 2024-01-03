/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
package com.axelor.auth.pac4j;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.client.finder.DefaultCallbackClientFinder;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.util.CommonHelper;
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
    if (result.isEmpty() && CommonHelper.isNotBlank(clientNames)) {
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
      logger.debug("Defaulting to the only client: {}", indirectClients.get(0));
      result.addAll(indirectClients);
    }

    return result;
  }

  private boolean isInitialized(Client client) {
    if (client instanceof BaseClient) {
      final BaseClient baseClient = ((BaseClient) client);
      return baseClient.getCredentialsExtractor() != null && baseClient.getAuthenticator() != null;
    }
    return true;
  }
}
