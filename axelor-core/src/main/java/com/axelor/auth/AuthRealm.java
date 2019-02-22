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

import com.axelor.auth.AuthFilter.UsernamePasswordTokenWithParams;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.inject.Beans;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.ExpiredCredentialsException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthRealm extends AuthorizingRealm {

  private static Logger log = LoggerFactory.getLogger(AuthRealm.class);

  private static final String INCORRECT_CREDENTIALS = /*$$(*/ "Wrong username or password"; /*)*/;
  private static final String WRONG_CURRENT_PASSWORD = /*$$(*/ "Wrong current password"; /*)*/;

  public static class AuthMatcher extends PasswordMatcher {

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {

      Object plain = getSubmittedPassword(token);
      Object saved = getStoredPassword(info);
      AuthService service = AuthService.getInstance();

      if (plain instanceof char[]) {
        plain = new String((char[]) plain);
      }

      final UsernamePasswordTokenWithParams userToken = (UsernamePasswordTokenWithParams) token;
      final UserAuthenticationInfo userInfo = (UserAuthenticationInfo) info;

      if (service.match((String) plain, (String) saved) || super.doCredentialsMatch(token, info)) {
        processPasswordChange(userToken, userInfo);
        return true;
      }

      if (isChangingPassword(userToken)) {
        throw new UserExpiredCredentialsException(userInfo.getUser(), WRONG_CURRENT_PASSWORD);
      }

      log.error("Password authentication failed for user: {}", token.getPrincipal());
      throw new IncorrectCredentialsException(INCORRECT_CREDENTIALS);
    }

    private boolean isChangingPassword(UsernamePasswordTokenWithParams token) {
      final String newPassword = token.getCleanParam("newPassword");
      return StringUtils.notBlank(newPassword);
    }

    private void processPasswordChange(
        UsernamePasswordTokenWithParams token, UserAuthenticationInfo info) {
      final User user = info.getUser();

      if (!user.getForcePasswordChange()) {
        return;
      }

      final String newPassword = token.getCleanParam("newPassword");

      if (StringUtils.isBlank(newPassword)) {
        throw new UserExpiredCredentialsException(user);
      }

      JPA.runInTransaction(
          () -> {
            Beans.get(AuthService.class).changePassword(user, newPassword);
            user.setForcePasswordChange(false);
          });
    }
  }

  private CredentialsMatcher credentialsMatcher = new AuthMatcher();

  @Override
  public CredentialsMatcher getCredentialsMatcher() {
    return credentialsMatcher;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token)
      throws AuthenticationException {

    final String code = ((UsernamePasswordToken) token).getUsername();
    final User user = AuthUtils.getUser(code);
    if (user == null || !AuthUtils.isActive(user)) {
      throw new IncorrectCredentialsException(INCORRECT_CREDENTIALS);
    }

    return new UserAuthenticationInfo(code, user.getPassword(), getName(), user);
  }

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {

    final String code = (String) principals.fromRealm(getName()).iterator().next();
    final User user = AuthUtils.getUser(code);

    if (user == null) {
      return null;
    }

    final SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
    final Group group = user.getGroup();
    if (group != null) {
      info.addRole(group.getCode());
    }

    return info;
  }

  static class UserExpiredCredentialsException extends ExpiredCredentialsException {
    private static final long serialVersionUID = 774688102294116466L;
    private final transient User user;

    public UserExpiredCredentialsException(User user) {
      this.user = user;
    }

    public UserExpiredCredentialsException(User user, String message) {
      super(message);
      this.user = user;
    }

    public User getUser() {
      return user;
    }
  }

  static class UserAuthenticationInfo extends SimpleAuthenticationInfo {
    private static final long serialVersionUID = 2404918058754102269L;
    private final transient User user;

    public UserAuthenticationInfo(
        Object principal, Object credentials, String realmName, User user) {
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
}
