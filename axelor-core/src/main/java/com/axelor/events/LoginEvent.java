/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.events;

import org.apache.shiro.authc.AuthenticationToken;

public abstract class LoginEvent {

  private final Object principal;

  private final Object credentials;

  public LoginEvent(AuthenticationToken token) {
    Object credentials = token.getCredentials();
    if (credentials instanceof char[] chars) {
      credentials = new String(chars);
    }
    this.principal = token.getPrincipal();
    this.credentials = credentials;
  }

  public Object getPrincipal() {
    return principal;
  }

  public Object getCredentials() {
    return credentials;
  }
}
