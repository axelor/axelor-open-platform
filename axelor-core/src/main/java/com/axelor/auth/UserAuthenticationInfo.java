/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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

import com.axelor.auth.db.User;
import org.apache.shiro.authc.SimpleAuthenticationInfo;

public class UserAuthenticationInfo extends SimpleAuthenticationInfo {
  private static final long serialVersionUID = 2404918058754102269L;
  private final transient User user;

  public UserAuthenticationInfo(Object principal, Object credentials, String realmName, User user) {
    super(principal, credentials, realmName);
    this.user = user;
  }

  public User getUser() {
    return user;
  }

  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
