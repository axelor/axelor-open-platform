/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.events;

import com.axelor.auth.db.User;

public class LogoutEvent {

  private final Object principal;

  private final User user;

  public LogoutEvent(Object principal, User user) {
    this.principal = principal;
    this.user = user;
  }

  public Object getPrincipal() {
    return principal;
  }

  public User getUser() {
    return user;
  }
}
