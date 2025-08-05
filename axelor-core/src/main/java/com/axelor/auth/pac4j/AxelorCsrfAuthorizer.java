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

import static org.pac4j.core.context.WebContextHelper.*;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Set;
import org.pac4j.core.authorization.authorizer.CsrfAuthorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.util.Pac4jConstants;

@Singleton
public class AxelorCsrfAuthorizer extends CsrfAuthorizer {
  public static final String CSRF_AUTHORIZER_NAME = "axelorCsrf";

  private final Set<String> directClients;

  @Inject
  public AxelorCsrfAuthorizer(ClientListService clientListService) {
    super(AuthPac4jModule.CSRF_HEADER_NAME, AuthPac4jModule.CSRF_HEADER_NAME);
    directClients = clientListService.getDirectClientNames();
  }

  /**
   * Checks if the user profiles and / or the current web context are authorized.
   *
   * <p>No CSRF check needed for native and direct clients.
   *
   * @param context the web context
   * @param sessionStore the session store
   * @param profiles the user profiles
   * @return if the access is authorized
   */
  @Override
  public boolean isAuthorized(
      WebContext context, SessionStore sessionStore, List<UserProfile> profiles) {

    // No CSRF check for native clients nor direct clients
    // We also authorize non-native direct clients
    if (AuthPac4jInfo.isNativeClient(context) || isDirectClient(profiles)) {
      return true;
    }

    var checkRequest = isPost(context) || isPut(context) || isPatch(context) || isDelete(context);
    if (checkRequest) {
      var parameterToken = context.getRequestParameter(getParameterName()).orElse(null);
      var headerToken = context.getRequestHeader(getHeaderName()).orElse(null);
      var sessionToken = sessionStore.get(context, Pac4jConstants.CSRF_TOKEN);
      var hasSessionData = sessionToken.isPresent();
      var token = (String) sessionToken.orElse(Pac4jConstants.EMPTY_STRING);
      var isGoodToken = hashEquals(token, parameterToken) || hashEquals(token, headerToken);
      if (!hasSessionData || !isGoodToken) {
        return false;
      }
    }
    return true;
  }

  private boolean isDirectClient(List<UserProfile> profiles) {
    return !profiles.isEmpty() && directClients.contains(profiles.getFirst().getClientName());
  }
}
