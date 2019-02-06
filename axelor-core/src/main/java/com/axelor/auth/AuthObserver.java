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
import com.google.inject.Singleton;
import javax.inject.Named;

@Singleton
public class AuthObserver {

  /**
   * Observes successful login events.
   *
   * @param event
   */
  void onPostLogin(@Observes @Named(PostLogin.SUCCESS) PostLogin event) {
    final User user = event.getUser();

    if (user.getForcePasswordChange()) {
      final String url =
          String.format("#/ds/action-auth-users-change-password/edit/%d", user.getId());
      throw new LoginRedirectException(url);
    }
  }
}
