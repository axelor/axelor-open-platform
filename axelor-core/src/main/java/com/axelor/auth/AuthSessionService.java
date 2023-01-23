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
package com.axelor.auth;

import java.time.LocalDateTime;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.session.Session;

/** Manages session attributes. */
class AuthSessionService {
  private static final String LOGIN_DATE = "com.axelor.internal.loginDate";

  public void updateLoginDate() {
    updateLoginDate(AuthUtils.getSubject().getSession());
  }

  public void updateLoginDate(Session session) {
    session.setAttribute(LOGIN_DATE, LocalDateTime.now());
  }

  public LocalDateTime getLoginDate() {
    return getLoginDate(AuthUtils.getSubject().getSession());
  }

  public LocalDateTime getLoginDate(Session session) {
    try {
      return (LocalDateTime) session.getAttribute(LOGIN_DATE);
    } catch (InvalidSessionException e) {
      return null;
    }
  }
}
