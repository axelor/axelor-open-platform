/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Set;
import org.pac4j.core.authorization.authorizer.CsrfAuthorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.UserProfile;

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

    return super.isAuthorized(context, sessionStore, profiles);
  }

  private boolean isDirectClient(List<UserProfile> profiles) {
    return !profiles.isEmpty() && directClients.contains(profiles.getFirst().getClientName());
  }
}
