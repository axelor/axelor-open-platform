/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import com.axelor.auth.AuthSessionService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import org.apache.shiro.SecurityUtils;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.UserProfile;

@Singleton
public class AxelorUserAuthorizer implements Authorizer {

  public static final String USER_AUTHORIZER_NAME = "AxelorUserAuthorizer";

  private AuthSessionService authSessionService;

  @Inject
  public AxelorUserAuthorizer(AuthSessionService authSessionService) {
    this.authSessionService = authSessionService;
  }

  @Override
  public boolean isAuthorized(
      WebContext context, SessionStore sessionStore, List<UserProfile> profiles) {
    User user = AuthUtils.getUser();
    if (user == null) {
      return false;
    }
    if (!AuthUtils.isActive(user)) {
      removeSession();
      return false;
    }

    return true;
  }

  private void removeSession() {
    try {
      authSessionService.terminateSession(SecurityUtils.getSubject());
    } catch (Exception e) {
      // ignore
    }
  }
}
