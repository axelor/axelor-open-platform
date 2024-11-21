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
package com.axelor.auth.pac4j;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.engine.DefaultSecurityLogic;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.matching.matcher.Matcher;
import org.pac4j.core.util.Pac4jConstants;

@Singleton
public class AxelorSecurityLogic extends DefaultSecurityLogic {

  private final ErrorHandler errorHandler;

  private final String defaultClientName;

  static final String HASH_LOCATION_PARAMETER = "hash_location";

  @Inject
  public AxelorSecurityLogic(ErrorHandler errorHandler, ClientListService clientListService) {
    this.errorHandler = errorHandler;
    defaultClientName = clientListService.getDefaultClientName();

    setAuthorizationChecker(
        (context, sessionStore, profiles, authorizerNames, authorizersMap, clients) ->
            authorizersMap.values().stream()
                .allMatch(authorizer -> authorizer.isAuthorized(context, sessionStore, profiles)));

    setMatchingChecker(
        (CallContext ctx,
            String matchersValue,
            Map<String, Matcher> matchersMap,
            List<Client> clients) ->
            matchersMap.values().stream().allMatch(matcher -> matcher.matches(ctx)));
  }

  @Override
  protected HttpAction redirectToIdentityProvider(
      final CallContext ctx, final List<Client> currentClients) {
    final var context = ctx.webContext();
    final var currentClient = (IndirectClient) findClient(context, currentClients);

    if (currentClient.getRedirectionActionBuilder() == null) {
      currentClient.init(true);
    }

    final Optional<RedirectionAction> action = currentClient.getRedirectionAction(ctx);
    return action.isPresent() ? action.get() : unauthorized(ctx, currentClients);
  }

  @Override
  protected Object handleException(
      Exception e, HttpActionAdapter httpActionAdapter, WebContext context) {
    return errorHandler.handleException(e, httpActionAdapter, context);
  }

  protected String getDefaultClient(WebContext context) {
    return context
        .getRequestParameter(Pac4jConstants.DEFAULT_FORCE_CLIENT_PARAMETER)
        .orElse(defaultClientName);
  }

  protected Client findClient(WebContext context, List<Client> clients) {
    final String defaultClient = getDefaultClient(context);
    return clients.stream()
        .filter(client -> defaultClient.equals(client.getName()))
        .findFirst()
        .orElseGet(() -> clients.get(0));
  }
}
