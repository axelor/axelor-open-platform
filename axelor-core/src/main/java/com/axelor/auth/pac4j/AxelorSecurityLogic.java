/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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

import io.buji.pac4j.profile.ShiroProfileManager;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.authorization.checker.DefaultAuthorizationChecker;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.DefaultSecurityLogic;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.core.http.ajax.AjaxRequestResolver;
import org.pac4j.core.matching.checker.DefaultMatchingChecker;
import org.pac4j.core.matching.matcher.Matcher;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.util.Pac4jConstants;

public class AxelorSecurityLogic extends DefaultSecurityLogic {

  private final AuthPac4jInfo authPac4jInfo;
  static final String HASH_LOCATION_PARAMETER = "hash_location";

  @Inject
  public AxelorSecurityLogic(
      AuthPac4jInfo authPac4jInfo,
      AxelorCsrfAuthorizer csrfAuthorizer,
      AxelorCsrfMatcher csrfMatcher) {
    this.authPac4jInfo = authPac4jInfo;
    setProfileManagerFactory(ShiroProfileManager::new);
    setAuthorizationChecker(
        new DefaultAuthorizationChecker() {

          @Override
          protected List<Authorizer> computeDefaultAuthorizers(
              WebContext context,
              List<UserProfile> profiles,
              List<Client> clients,
              Map<String, Authorizer> authorizersMap) {

            if (clients.stream().anyMatch(IndirectClient.class::isInstance)) {
              return List.of(csrfAuthorizer);
            }

            return Collections.emptyList();
          }
        });
    setMatchingChecker(
        new DefaultMatchingChecker() {

          @Override
          protected List<Matcher> computeDefaultMatchers(
              WebContext context, SessionStore sessionStore, List<Client> clients) {

            if (clients.stream().anyMatch(IndirectClient.class::isInstance)) {
              return List.of(csrfMatcher);
            }

            return Collections.emptyList();
          }
        });
  }

  // Don't save requested URL if redirected to a non-default central client,
  // so that the requested URL saved before redirection will be used instead.
  @Override
  protected void saveRequestedUrl(
      WebContext context,
      SessionStore sessionStore,
      List<Client> currentClients,
      AjaxRequestResolver ajaxRequestResolver) {

    context
        .getRequestParameter(HASH_LOCATION_PARAMETER)
        .ifPresent(
            hashLocation -> sessionStore.set(context, HASH_LOCATION_PARAMETER, hashLocation));

    if (context.getRequestParameter(Pac4jConstants.DEFAULT_FORCE_CLIENT_PARAMETER).isEmpty()
        || currentClients.size() != 1
        || !authPac4jInfo.getCentralClients().contains(currentClients.get(0).getName())) {
      super.saveRequestedUrl(context, sessionStore, currentClients, ajaxRequestResolver);
    }
  }

  @Override
  protected HttpAction redirectToIdentityProvider(
      WebContext context, SessionStore sessionStore, List<Client> currentClients) {

    final var currentClient = (IndirectClient) currentClients.get(0);

    if (currentClient.getRedirectionActionBuilder() == null) {
      currentClient.init(true);
    }

    final Optional<RedirectionAction> action =
        currentClient.getRedirectionAction(context, sessionStore);
    return action.isPresent() ? action.get() : unauthorized(context, sessionStore, currentClients);
  }
}
