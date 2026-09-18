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

  /**
   * Fires the post-login success event, or rejects the login when the authenticated profile has no
   * matching active user.
   *
   * <p>{@link AuthPac4jRealm} doesn't fail authentication itself when no active user matches the
   * profile: authentication did succeed as far as the identity provider is concerned, only the
   * local user is missing. It returns a plain {@link AuthenticationInfo} instead of a {@link
   * UserAuthenticationInfo}, which is detected here to fire the post-login failure event, terminate
   * the session and abort the login.
   *
   * @param token the token used to authenticate
   * @param info the authentication info built by the realm
   * @throws UnknownAccountException if no active user matches the authenticated profile
   */
  @Override
  public void onSuccess(AuthenticationToken token, AuthenticationInfo info) {
    if (info instanceof UserAuthenticationInfo authenticationInfo) {
      final User user = authenticationInfo.getUser();

      if (user != null) {
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
    sessionService.terminateSession(SecurityUtils.getSubject());

    throw exception;
  }

  /**
   * Not expected to be reached: login failures are reported elsewhere.
   *
   * <p>Credentials are validated by the pac4j clients, before Shiro is involved. A rejection there
   * never produces a token nor reaches any realm, so no Shiro authentication listener is notified
   * and the failure event is fired by {@link com.axelor.auth.pac4j.local.CredentialsHandler}
   * instead.
   *
   * <p>Logins rejected by {@link #onSuccess(AuthenticationToken, AuthenticationInfo)} don't come
   * here either: Shiro calls success listeners outside the block that notifies failure listeners,
   * so the exception thrown there propagates directly out of the login. Hence that rejection fires
   * the failure event itself.
   *
   * <p>This would only be called if a realm threw an {@link AuthenticationException}, which {@link
   * AuthPac4jRealm} doesn't do.
   *
   * @param token the token used to authenticate
   * @param ae the exception thrown by the realm
   */
  @Override
  public void onFailure(AuthenticationToken token, AuthenticationException ae) {
    // Nothing to do: see javadoc.
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
