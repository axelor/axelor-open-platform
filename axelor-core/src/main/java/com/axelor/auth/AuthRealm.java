/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth;

import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
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

  private static final String INCORRECT_CREDENTIALS = /*$$(*/ "Wrong username or password" /*)*/;

  public static class AuthMatcher extends PasswordMatcher {

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {

      Object plain = getSubmittedPassword(token);
      Object saved = getStoredPassword(info);
      AuthService service = AuthService.getInstance();

      if (plain instanceof char[] chars) {
        plain = new String(chars);
      }

      if (service.match((String) plain, (String) saved) || super.doCredentialsMatch(token, info)) {
        return true;
      }

      log.error("Password authentication failed for user: {}", token.getPrincipal());
      throw new IncorrectCredentialsException(INCORRECT_CREDENTIALS);
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
}
