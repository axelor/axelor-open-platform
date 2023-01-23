/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.auth.pac4j;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.UserAuthenticationInfo;
import com.axelor.auth.db.User;
import com.axelor.event.Event;
import com.axelor.event.NamedLiteral;
import com.axelor.events.LogoutEvent;
import com.axelor.events.PostLogin;
import java.util.Optional;
import javax.inject.Inject;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationListener;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.subject.PrincipalCollection;
import org.pac4j.core.profile.CommonProfile;

public class AuthPac4jListener implements AuthenticationListener {

  @Inject private Event<PostLogin> postLoginEvent;
  @Inject private Event<LogoutEvent> logoutEvent;
  @Inject private AuthPac4jProfileService profileService;
  @Inject private AxelorSessionManager sessionManager;

  private static final String UNKNOWN_USER = "User not found: %s";

  @Override
  public void onSuccess(AuthenticationToken token, AuthenticationInfo info) {
    if (info instanceof UserAuthenticationInfo) {
      final User user = ((UserAuthenticationInfo) info).getUser();

      if (user != null) {
        sessionManager.changeSessionId();
        firePostLoginSuccess(token, user);
        return;
      }
    }

    @SuppressWarnings("unchecked")
    final Optional<CommonProfile> profile = (Optional<CommonProfile>) token.getPrincipal();
    final String username =
        profile
            .map(profileService::getUserIdentifier)
            .orElseGet(() -> String.valueOf(token.getPrincipal()));
    final String msg = String.format(UNKNOWN_USER, username);
    final UnknownAccountException exception = new UnknownAccountException(msg);

    firePostLoginFailure(token, exception);
    SecurityUtils.getSubject().logout();

    throw exception;
  }

  @Override
  public void onFailure(AuthenticationToken token, AuthenticationException ae) {
    // Login failure handled by {@link com.axelor.auth.pac4j.local.CredentialsHandler}
  }

  @Override
  public void onLogout(PrincipalCollection principals) {
    logoutEvent.fire(new LogoutEvent(principals, AuthUtils.getUser()));
  }

  private void firePostLoginSuccess(AuthenticationToken token, User user) {
    postLoginEvent
        .select(NamedLiteral.of(PostLogin.SUCCESS))
        .fire(new PostLogin(token, user, null));
  }

  private void firePostLoginFailure(AuthenticationToken token, AuthenticationException ae) {
    postLoginEvent.select(NamedLiteral.of(PostLogin.FAILURE)).fire(new PostLogin(token, null, ae));
  }
}
