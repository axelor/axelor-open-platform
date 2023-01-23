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
package com.axelor.web;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/** The {@link AppSessionListener} configures the session timeout. */
public final class AppSessionListener implements HttpSessionListener {

  private final int timeout;

  private static final Set<HttpSession> sessions = ConcurrentHashMap.newKeySet();

  /** Create a new {@link AppSessionListener} with the given app settings. */
  public AppSessionListener() {
    this.timeout = AppSettings.get().getInt(AvailableAppSettings.SESSION_TIMEOUT, 60);
  }

  @Override
  public void sessionCreated(HttpSessionEvent event) {
    final HttpSession session = event.getSession();
    sessions.add(session);
    session.setMaxInactiveInterval(timeout * 60);
  }

  @Override
  public void sessionDestroyed(HttpSessionEvent event) {
    final HttpSession session = event.getSession();
    sessions.remove(session);
  }

  public static Set<HttpSession> getSessions() {
    return sessions;
  }

  @Deprecated
  public static Set<String> getActiveSessions() {
    return sessions.stream().map(HttpSession::getId).collect(Collectors.toSet());
  }

  @Deprecated
  public static HttpSession getSession(String id) {
    return sessions.stream()
        .filter(session -> Objects.equals(session.getId(), id))
        .findAny()
        .orElse(null);
  }
}
