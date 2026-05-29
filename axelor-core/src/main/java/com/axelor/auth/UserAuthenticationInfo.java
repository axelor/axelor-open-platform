/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth;

import com.axelor.auth.db.User;
import java.util.List;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.subject.SimplePrincipalCollection;

public class UserAuthenticationInfo extends SimpleAuthenticationInfo {
  private static final long serialVersionUID = 2404918058754102269L;
  private final transient User user;

  public UserAuthenticationInfo(Object principal, Object credentials, String realmName, User user) {
    super(new SimplePrincipalCollection(List.of(principal, user.getId()), realmName), credentials);
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
