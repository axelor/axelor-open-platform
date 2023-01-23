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

import java.util.function.Supplier;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.servlet.Cookie;
import org.apache.shiro.web.servlet.Cookie.SameSiteOptions;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.util.WebUtils;

/**
 * RememberMe Manager
 *
 * <p>This implements all of {@link org.apache.shiro.mgt.RememberMeManager} interface and uses
 * SameSite attribute for secure requests.
 */
@Singleton
public class AxelorRememberMeManager extends CookieRememberMeManager {
  private final Cookie secureCookie;
  private final ThreadLocal<HttpServletRequest> currentRequest = new ThreadLocal<>();

  public AxelorRememberMeManager() {
    secureCookie = new SimpleCookie(super.getCookie());
    secureCookie.setSecure(true);
    secureCookie.setSameSite(SameSiteOptions.NONE);
  }

  @Override
  public PrincipalCollection getRememberedPrincipals(SubjectContext subjectContext) {
    return withRequest(subjectContext, () -> super.getRememberedPrincipals(subjectContext));
  }

  @Override
  public void forgetIdentity(SubjectContext subjectContext) {
    withRequest(subjectContext, () -> super.forgetIdentity(subjectContext));
  }

  @Override
  public void onSuccessfulLogin(
      Subject subject, AuthenticationToken token, AuthenticationInfo info) {
    withRequest(subject, () -> super.onSuccessfulLogin(subject, token, info));
  }

  @Override
  public void onFailedLogin(
      Subject subject, AuthenticationToken token, AuthenticationException ae) {
    withRequest(subject, () -> super.onFailedLogin(subject, token, ae));
  }

  @Override
  public void onLogout(Subject subject) {
    withRequest(subject, () -> super.onLogout(subject));
  }

  @Override
  public Cookie getCookie() {
    final HttpServletRequest request = getRequest();
    return request != null && request.isSecure() ? secureCookie : super.getCookie();
  }

  protected HttpServletRequest getRequest() {
    return currentRequest.get();
  }

  protected <T> T withRequest(Object requestPairSource, Supplier<T> task) {
    final HttpServletRequest request = WebUtils.getHttpRequest(requestPairSource);
    currentRequest.set(request);
    try {
      return task.get();
    } finally {
      currentRequest.remove();
    }
  }

  protected void withRequest(Object requestPairSource, Runnable task) {
    withRequest(
        requestPairSource,
        () -> {
          task.run();
          return null;
        });
  }
}
