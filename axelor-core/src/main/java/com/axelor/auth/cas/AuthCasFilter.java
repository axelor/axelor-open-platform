/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
package com.axelor.auth.cas;

import com.axelor.auth.AuthUtils;
import com.axelor.event.Event;
import com.axelor.event.NamedLiteral;
import com.axelor.events.LoginRedirectException;
import com.axelor.events.PostLogin;
import com.axelor.events.PreLogin;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.cas.CasFilter;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.util.WebUtils;

public class AuthCasFilter extends CasFilter {

  @Inject private Event<PreLogin> preLogin;
  @Inject private Event<PostLogin> postLogin;

  @Override
  protected boolean executeLogin(ServletRequest request, ServletResponse response)
      throws Exception {
    AuthenticationToken token = createToken(request, response);
    if (token == null) {
      String msg =
          "createToken method implementation returned null. A valid non-null AuthenticationToken "
              + "must be created in order to execute a login attempt.";
      throw new IllegalStateException(msg);
    }
    try {
      try {
        preLogin.fire(new PreLogin(token));
        Subject subject = getSubject(request, response);
        subject.login(token);
        postLogin
            .select(NamedLiteral.of(PostLogin.SUCCESS))
            .fire(new PostLogin(token, AuthUtils.getUser(), null));
        return onLoginSuccess(token, subject, request, response);
      } catch (AuthenticationException e) {
        postLogin.select(NamedLiteral.of(PostLogin.FAILURE)).fire(new PostLogin(token, null, e));
        return onLoginFailure(token, e, request, response);
      }
    } catch (LoginRedirectException e) {
      WebUtils.issueRedirect(request, response, e.getLocation());
      return false;
    }
  }

  @Inject
  @Override
  public void setFailureUrl(@Named("shiro.cas.failure.url") String failureUrl) {
    super.setFailureUrl(failureUrl);
  }
}
