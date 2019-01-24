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
package com.axelor.auth.ldap;

import com.axelor.auth.AuthRealm;
import com.axelor.inject.Beans;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthLdapRealm extends AuthRealm {

  private static Logger log = LoggerFactory.getLogger(AuthLdapRealm.class);

  public static class AuthMatcher extends PasswordMatcher {

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {

      Object plain = getSubmittedPassword(token);
      AuthLdapService service = Beans.get(AuthLdapService.class);

      if (plain instanceof char[]) {
        plain = new String((char[]) plain);
      }

      try {
        return service.login((String) token.getPrincipal(), (String) plain);
      } catch (Exception e) {
        log.error("Authentication failed for user: {}", token.getPrincipal());
        return false;
      }
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
    final String passwd = new String(((UsernamePasswordToken) token).getPassword());
    final AuthLdapService service = Beans.get(AuthLdapService.class);

    try {
      service.login(code, passwd);
    } catch (IllegalStateException e) {
    } catch (AuthenticationException e) {
      log.error("LDAP authentication failed for user: {}", code);
    }

    return super.doGetAuthenticationInfo(token);
  }
}
