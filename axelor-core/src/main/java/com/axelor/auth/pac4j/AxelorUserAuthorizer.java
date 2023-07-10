/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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

import com.axelor.auth.AuthSessionService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import java.time.LocalDateTime;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
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
    if (!isAllowed(user)) {
      removeSession();
      return false;
    }

    return true;
  }

  private boolean isAllowed(User user) {
    final LocalDateTime loginDate =
        authSessionService.getLoginDate(AuthUtils.getSubject().getSession());
    return AuthUtils.isActive(user)
        && (user.getPasswordUpdatedOn() == null
            || loginDate != null && !loginDate.isBefore(user.getPasswordUpdatedOn()));
  }

  private void removeSession() {
    try {
      SecurityUtils.getSubject().logout();
    } catch (Exception e) {
      // ignore
    }
  }
}
