/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.auth;

import com.axelor.auth.db.User;
import com.axelor.event.Observes;
import com.axelor.events.LoginRedirectException;
import com.axelor.events.PostLogin;
import com.axelor.events.PreRequest;
import com.axelor.i18n.I18n;
import com.google.inject.Singleton;
import java.time.LocalDateTime;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.subject.Subject;

/** Observes authentication-related events. */
@Singleton
public class AuthObserver {

  @Inject private AuthSessionService authSessionService;

  /**
   * Observes successful login events.
   *
   * @param event
   */
  void onPostLogin(@Observes @Named(PostLogin.SUCCESS) PostLogin event) {
    authSessionService.updateLoginDate();

    final User user = event.getUser();

    if (user.getForcePasswordChange()) {
      throw new LoginRedirectException("#/change-password");
    }
  }

  /**
   * Observes all pre-requests and log out users that logged in before password change date.
   *
   * @param event
   */
  void onPreRequest(@Observes PreRequest event) {
    final Subject subject = AuthUtils.getSubject();

    if (subject != null) {
      final User user = AuthUtils.getUser();
      final LocalDateTime loginDate = authSessionService.getLoginDate(subject.getSession());

      if (user != null
          && (user.getPasswordUpdatedOn() == null
              || loginDate != null && !loginDate.isBefore(user.getPasswordUpdatedOn()))) {
        return;
      }

      subject.logout();
    }

    throw new UnauthenticatedException(I18n.get("Please log back in."));
  }
}
