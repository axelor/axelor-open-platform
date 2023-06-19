/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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

import com.axelor.auth.AuthSessionService;
import com.axelor.auth.UserAuthenticationInfo;
import com.axelor.auth.db.User;
import com.axelor.event.Event;
import com.axelor.event.NamedLiteral;
import com.axelor.events.LoginRedirectException;
import com.axelor.events.PostLogin;
import com.axelor.inject.Beans;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationListener;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthPac4jListener implements AuthenticationListener {

  @Inject private Event<PostLogin> postLogin;
  @Inject private AuthSessionService sessionService;

  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void onSuccess(AuthenticationToken token, AuthenticationInfo info) {
    if (info instanceof UserAuthenticationInfo) {
      final User user = ((UserAuthenticationInfo) info).getUser();

      if (user != null) {
        Beans.get(HttpServletRequest.class).changeSessionId();
        sessionService.updateLoginDate();
        firePostLoginSuccess(token, user);
        return;
      }
    }

    logger.error("No active user found for principal: {}", token.getPrincipal());
    firePostLoginFailure(token, new UnknownAccountException(info.toString()));
    SecurityUtils.getSubject().logout();
  }

  @Override
  public void onFailure(AuthenticationToken token, AuthenticationException ae) {
    // Login failure handled by {@link com.axelor.auth.pac4j.AuthPac4jCredentialsHandler}
  }

  @Override
  public void onLogout(PrincipalCollection principals) {
    // TODO: implement logout event
  }

  private void firePostLoginSuccess(AuthenticationToken token, User user) {
    try {
      postLogin.select(NamedLiteral.of(PostLogin.SUCCESS)).fire(new PostLogin(token, user, null));
    } catch (LoginRedirectException e) {
      issueRedirect(e.getLocation());
    }
  }

  private void firePostLoginFailure(AuthenticationToken token, AuthenticationException ae) {
    try {
      postLogin.select(NamedLiteral.of(PostLogin.FAILURE)).fire(new PostLogin(token, null, ae));
    } catch (LoginRedirectException e) {
      issueRedirect(e.getLocation());
    }
  }

  private void issueRedirect(String url) {
    try {
      WebUtils.issueRedirect(
          Beans.get(HttpServletRequest.class), Beans.get(HttpServletResponse.class), url);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
