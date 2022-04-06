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
import java.util.List;
import javax.inject.Inject;
import org.pac4j.core.authorization.authorizer.DefaultAuthorizers;
import org.pac4j.core.authorization.checker.DefaultAuthorizationChecker;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.context.JEEContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.engine.DefaultSecurityLogic;
import org.pac4j.core.http.ajax.AjaxRequestResolver;
import org.pac4j.core.matching.checker.DefaultMatchingChecker;
import org.pac4j.core.matching.matcher.DefaultMatchers;
import org.pac4j.core.util.Pac4jConstants;

public class AxelorSecurityLogic extends DefaultSecurityLogic<Object, JEEContext> {

  private final AuthPac4jInfo authPac4jInfo;
  static final String HASH_LOCATION_PARAMETER = "hash_location";

  @Inject
  public AxelorSecurityLogic(AuthPac4jInfo authPac4jInfo) {
    this.authPac4jInfo = authPac4jInfo;
    setProfileManagerFactory(ShiroProfileManager::new);
    setAuthorizationChecker(
        new DefaultAuthorizationChecker() {

          @Override
          protected String computeDefaultAuthorizers(List<Client<? extends Credentials>> clients) {
            if (clients.stream().anyMatch(IndirectClient.class::isInstance)) {
              return AxelorCsrfAuthorizer.CSRF_AUTHORIZER_NAME;
            }
            return DefaultAuthorizers.NONE;
          }
        });
    setMatchingChecker(
        new DefaultMatchingChecker() {

          @Override
          protected String computeDefaultMatchers(List<Client<? extends Credentials>> clients) {
            if (clients.stream().anyMatch(IndirectClient.class::isInstance)) {
              return AxelorCsrfMatcher.CSRF_MATCHER_NAME;
            }

            return DefaultMatchers.NONE;
          }
        });
  }

  // Don't save requested URL if redirected to a non-default central client,
  // so that the requested URL saved before redirection will be used instead.
  @Override
  protected void saveRequestedUrl(
      JEEContext context,
      List<Client<? extends Credentials>> currentClients,
      AjaxRequestResolver ajaxRequestResolver) {

    context
        .getRequestParameter(HASH_LOCATION_PARAMETER)
        .ifPresent(
            hashLocation -> {
              @SuppressWarnings("unchecked")
              final SessionStore<JEEContext> sessionStore = context.getSessionStore();
              sessionStore.set(context, HASH_LOCATION_PARAMETER, hashLocation);
            });

    if (context.getRequestParameter(Pac4jConstants.DEFAULT_CLIENT_NAME_PARAMETER).isEmpty()
        || currentClients.size() != 1
        || !authPac4jInfo.getCentralClients().contains(currentClients.get(0).getName())) {
      super.saveRequestedUrl(context, currentClients, ajaxRequestResolver);
    }
  }
}
