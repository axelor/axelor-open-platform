/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import com.axelor.auth.AuthSessionService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.UserAuthenticationInfo;
import com.axelor.auth.db.User;
import com.axelor.event.Event;
import com.axelor.event.NamedLiteral;
import com.axelor.events.LogoutEvent;
import com.axelor.events.PostLogin;
import jakarta.inject.Inject;
import java.util.Optional;
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
  @Inject private AuthSessionService sessionService;

  private static final String UNKNOWN_USER = "User not found: %s";

  @Override
  public void onSuccess(AuthenticationToken token, AuthenticationInfo info) {
    if (info instanceof UserAuthenticationInfo authenticationInfo) {
      final User user = authenticationInfo.getUser();

      if (user != null) {
        sessionService.updateLoginDate();
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
    final String msg = UNKNOWN_USER.formatted(username);
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
