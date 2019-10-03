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
package com.axelor.auth.pac4j;

import com.axelor.auth.UserAuthenticationInfo;
import com.axelor.auth.db.User;
import com.axelor.event.Event;
import com.axelor.event.NamedLiteral;
import com.axelor.events.PostLogin;
import java.lang.invoke.MethodHandles;
import javax.inject.Inject;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationListener;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthPac4jListener implements AuthenticationListener {

  @Inject private Event<PostLogin> postLogin;

  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void onSuccess(AuthenticationToken token, AuthenticationInfo info) {
    if (info instanceof UserAuthenticationInfo) {
      final User user = ((UserAuthenticationInfo) info).getUser();

      if (user != null) {
        firePostLoginSuccess(token, user);
        return;
      }
    }

    logger.error("No user found for principal: {}", token.getPrincipal());
    firePostLoginFailure(token, new UnknownAccountException(info.toString()));
    SecurityUtils.getSubject().logout();
  }

  @Override
  public void onFailure(AuthenticationToken token, AuthenticationException ae) {
    logger.error("Authentication failed for principal: {}", token.getPrincipal());
    firePostLoginFailure(token, ae);
  }

  @Override
  public void onLogout(PrincipalCollection principals) {
    // TODO: implement logout event
  }

  private void firePostLoginSuccess(AuthenticationToken token, User user) {
    postLogin.select(NamedLiteral.of(PostLogin.SUCCESS)).fire(new PostLogin(token, user, null));
  }

  private void firePostLoginFailure(AuthenticationToken token, AuthenticationException ae) {
    postLogin.select(NamedLiteral.of(PostLogin.FAILURE)).fire(new PostLogin(token, null, ae));
  }
}
