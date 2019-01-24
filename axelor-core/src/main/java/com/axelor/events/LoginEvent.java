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
package com.axelor.events;

import org.apache.shiro.authc.AuthenticationToken;

public abstract class LoginEvent {

  private final String principal;

  private final String credentials;

  public LoginEvent(AuthenticationToken token) {
    Object credentials = token.getCredentials();
    if (credentials instanceof char[]) {
      credentials = new String((char[]) credentials);
    }
    this.principal = (String) token.getPrincipal();
    this.credentials = (String) credentials;
  }

  public String getPrincipal() {
    return principal;
  }

  public String getCredentials() {
    return credentials;
  }
}
