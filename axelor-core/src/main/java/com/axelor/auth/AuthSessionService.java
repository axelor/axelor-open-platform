/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth;

import com.axelor.inject.Beans;
import jakarta.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.Collection;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.SessionDAO;

/** Manages session attributes. */
public class AuthSessionService {
  private static final String LOGIN_DATE = "com.axelor.internal.loginDate";

  public void updateLoginDate() {
    updateLoginDate(AuthUtils.getSubject().getSession(false));
  }

  public void updateLoginDate(Session session) {
    if (session != null) {
      session.setAttribute(LOGIN_DATE, LocalDateTime.now());
    }
  }

  @Nullable
  public LocalDateTime getLoginDate() {
    return getLoginDate(AuthUtils.getSubject().getSession(false));
  }

  @Nullable
  public LocalDateTime getLoginDate(Session session) {
    try {
      if (session != null) {
        return (LocalDateTime) session.getAttribute(LOGIN_DATE);
      }
    } catch (InvalidSessionException e) {
      // Fall through
    }

    return null;
  }

  public Collection<Session> getActiveSessions() {
    return Beans.get(SessionDAO.class).getActiveSessions();
  }
}
