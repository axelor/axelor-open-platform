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
import com.axelor.events.PostRequest;
import com.axelor.events.PreRequest;
import com.axelor.events.RequestEvent;
import com.axelor.events.qualifiers.EntityType;
import com.axelor.i18n.I18n;
import com.axelor.rpc.RequestUtils;
import com.google.inject.Singleton;
import java.time.LocalDateTime;
import java.util.Map;
import javax.inject.Named;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;

@Singleton
public class AuthObserver {

  private static final String LOGIN_DATE = "loginDate";

  /**
   * Observes all pre-requests and log out users that logged in before password change date.
   *
   * @param event
   */
  void onPreRequest(@Observes PreRequest event) {
    final Subject subject = AuthUtils.getSubject();

    if (subject != null) {
      final User user = AuthUtils.getUser();
      final LocalDateTime loginDate = getLoginDate(subject.getSession());

      if (user != null
          && (user.getPasswordUpdatedOn() == null
              || loginDate != null && !loginDate.isBefore(user.getPasswordUpdatedOn()))) {
        return;
      }

      subject.logout();
    }

    throw new UnauthenticatedException(I18n.get("Please log back in."));
  }

  /**
   * Updates password date field if password or blocked fields have changed.
   *
   * @param event
   */
  void onPreSaveUser(@Observes @Named(RequestEvent.SAVE) @EntityType(User.class) PreRequest event) {
    RequestUtils.processRequest(
        event.getRequest(),
        values -> {
          if (userPasswordHasChanged(values)) {
            values.put("passwordUpdatedOn", LocalDateTime.now());
          }
        });
  }

  /**
   * Updates current session's last login after saving user.
   *
   * @param event
   */
  void onPostSaveUser(
      @Observes @Named(RequestEvent.SAVE) @EntityType(User.class) PostRequest event) {
    final User user = AuthUtils.getUser();
    final Subject subject = AuthUtils.getSubject();

    if (user == null || subject == null) {
      return;
    }

    final Long userId = user.getId();
    final Session session = AuthUtils.getSubject().getSession();

    RequestUtils.processRequest(
        event.getRequest(),
        values -> {
          if (userId.equals(getId(values)) && userPasswordHasChanged(values)) {
            updateLoginDate(session);
          }
        });
  }

  /**
   * Observes successful login events.
   *
   * @param event
   */
  void onPostLogin(@Observes @Named(PostLogin.SUCCESS) PostLogin event) {
    final Session session = AuthUtils.getSubject().getSession();
    updateLoginDate(session);

    final User user = event.getUser();

    if (user.getForcePasswordChange()) {
      throw new LoginRedirectException("#/change-password");
    }
  }

  private Long getId(Map<String, Object> values) {
    final Number number = (Number) values.get("id");
    return number != null ? number.longValue() : null;
  }

  private boolean userPasswordHasChanged(Map<String, Object> values) {
    return values.get("newPassword") != null || values.get("blocked") != null;
  }

  private void updateLoginDate(Session session) {
    session.setAttribute(LOGIN_DATE, LocalDateTime.now());
  }

  private LocalDateTime getLoginDate(Session session) {
    try {
      return (LocalDateTime) session.getAttribute(LOGIN_DATE);
    } catch (InvalidSessionException e) {
      return null;
    }
  }
}
