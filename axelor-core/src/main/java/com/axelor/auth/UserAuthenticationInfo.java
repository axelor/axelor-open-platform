/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
