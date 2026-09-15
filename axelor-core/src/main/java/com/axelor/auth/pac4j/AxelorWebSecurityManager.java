/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.Set;
import org.apache.shiro.authc.AuthenticationListener;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.cache.jcache.AxelorJCacheManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.util.WebUtils;

/**
 * Web Security Manager
 *
 * <p>Since Shiro 2.2, a successful login stops the existing session and creates a new one to guard
 * against session fixation. The attributes of the old session are copied to the new one.
 *
 * <p>This implementation adapts that behavior:
 *
 * <ul>
 *   <li>Session-less clients disable session creation for the request, so the attempt to recreate
 *       the session would throw {@link org.apache.shiro.subject.support.DisabledSessionException}
 *       once the old one is stopped. The session renewal is skipped for them, and {@link
 *       AxelorWebSessionStorageEvaluator} prevents their login from being stored in the existing
 *       session.
 *   <li>The CSRF token is not carried over to the renewed session, so that a token issued before
 *       authentication cannot be used afterwards. A new token is generated on the login response.
 * </ul>
 */
@Singleton
public class AxelorWebSecurityManager extends DefaultWebSecurityManager {

  @Inject
  public AxelorWebSecurityManager(
      Collection<Realm> realms,
      Set<AuthenticationListener> authenticationListeners,
      ModularRealmAuthenticator authenticator,
      AxelorSessionManager sessionManager,
      AxelorRememberMeManager rememberMeManager,
      AxelorJCacheManager cacheManager) {
    setCacheManager(cacheManager);
    setRealms(realms);
    authenticator.setRealms(getRealms());
    authenticator.setAuthenticationListeners(authenticationListeners);
    setAuthenticator(authenticator);
    setSessionManager(sessionManager);
    setRememberMeManager(rememberMeManager);
  }

  @Override
  protected void beforeSuccessfulLogin(Subject subject) {
    if (WebUtils.isSessionCreationEnabled(subject)) {
      super.beforeSuccessfulLogin(subject);
    }
  }
}
