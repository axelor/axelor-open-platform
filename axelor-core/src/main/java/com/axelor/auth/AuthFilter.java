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
package com.axelor.auth;

import com.axelor.auth.AuthRealm.UserExpiredCredentialsException;
import com.axelor.common.StringUtils;
import com.axelor.event.Event;
import com.axelor.event.NamedLiteral;
import com.axelor.events.LoginRedirectException;
import com.axelor.events.PostLogin;
import com.axelor.events.PreLogin;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.apache.shiro.web.util.WebUtils;

public class AuthFilter extends FormAuthenticationFilter {

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

  @Override
  protected AuthenticationToken createToken(
      String username, String password, ServletRequest request, ServletResponse response) {
    boolean rememberMe = isRememberMe(request);
    String host = getHost(request);
    return new UsernamePasswordTokenWithParams(username, password, rememberMe, host, request);
  }

  private boolean isRootWithoutSlash(ServletRequest request) {
    final HttpServletRequest req = (HttpServletRequest) request;
    final String ctx = WebUtils.getContextPath(req);
    final String uri = WebUtils.getRequestUri(req);
    return ctx != null && uri != null && !uri.endsWith("/") && ctx.length() == uri.length();
  }

  @Override
  protected boolean isLoginRequest(ServletRequest request, ServletResponse response) {
    return super.isLoginRequest(request, response) || pathsMatch("/callback", request);
  }

  @Override
  public void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    // tomcat 7.0.67 doesn't redirect with / if root request is sent without slash
    // see RM-4500 for more details
    if (!SecurityUtils.getSubject().isAuthenticated() && isRootWithoutSlash(request)) {
      WebUtils.issueRedirect(request, response, "/");
      return;
    }

    if (isLoginRequest(request, response) && SecurityUtils.getSubject().isAuthenticated()) {
      // in case of login submission with ajax
      if (isXHR(request) && isLoginSubmission(request, response)) {
        WebUtils.toHttp(response).setStatus(200);
        return;
      }
      WebUtils.issueRedirect(request, response, "/");
    }
    super.doFilterInternal(request, response, chain);
  }

  @Override
  protected boolean onAccessDenied(ServletRequest request, ServletResponse response)
      throws Exception {

    // set encoding to UTF-8 (see RM-4304)
    request.setCharacterEncoding("UTF-8");

    if (isXHR(request)) {

      int status = 401;
      if (isLoginRequest(request, response) && isLoginSubmission(request, response)) {
        if (doLogin(request, response)) {
          status = 200;
        }
      }

      // set HTTP status for ajax requests
      ((HttpServletResponse) response).setStatus(status);

      // don't process further, otherwise login.jsp will be rendered as response data
      return false;
    }

    return super.onAccessDenied(request, response);
  }

  @Override
  protected boolean onLoginSuccess(
      AuthenticationToken token, Subject subject, ServletRequest request, ServletResponse response)
      throws Exception {
    // change session id to prevent session fixation
    ((HttpServletRequest) request).changeSessionId();
    return super.onLoginSuccess(token, subject, request, response);
  }

  @Override
  protected boolean onLoginFailure(
      AuthenticationToken token,
      AuthenticationException e,
      ServletRequest request,
      ServletResponse response) {

    if (e instanceof IncorrectCredentialsException) {
      final UriBuilder builder = UriBuilder.fromPath("login.jsp");

      if (StringUtils.notBlank(e.getMessage())) {
        builder.queryParam("error", e.getMessage());
      }

      final String path = builder.build().toString();
      forward(path, request, response);
    } else if (e instanceof UserExpiredCredentialsException) {
      final String username = ((UserExpiredCredentialsException) e).getUser().getCode();
      final UriBuilder builder =
          UriBuilder.fromPath("change-password.jsp").queryParam("username", username);

      if (StringUtils.notBlank(e.getMessage())) {
        builder.queryParam("error", e.getMessage());
      }

      final String path = builder.build().toString();
      forward(path, request, response);
    }

    return super.onLoginFailure(token, e, request, response);
  }

  private void forward(String path, ServletRequest request, ServletResponse response) {
    try {
      ((HttpServletRequest) request).getRequestDispatcher(path).forward(request, response);
    } catch (ServletException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private boolean doLogin(ServletRequest request, ServletResponse response) throws Exception {

    final ObjectMapper mapper = new ObjectMapper();
    final Map<String, String> data = mapper.readValue(request.getInputStream(), Map.class);

    final String username = data.get("username");
    final String password = data.get("password");

    final AuthenticationToken token = createToken(username, password, request, response);
    final Subject subject = getSubject(request, response);

    try {
      preLogin.fire(new PreLogin(token));
      subject.login(token);
      postLogin
          .select(NamedLiteral.of(PostLogin.SUCCESS))
          .fire(new PostLogin(token, AuthUtils.getUser(), null));
    } catch (AuthenticationException e) {
      postLogin.select(NamedLiteral.of(PostLogin.FAILURE)).fire(new PostLogin(token, null, e));
      return false;
    }

    return true;
  }

  private boolean isXHR(ServletRequest request) {
    final HttpServletRequest req = (HttpServletRequest) request;
    return "XMLHttpRequest".equals(req.getHeader("X-Requested-With"))
        || "application/json".equals(req.getHeader("Accept"))
        || "application/json".equals(req.getHeader("Content-Type"));
  }

  static class UsernamePasswordTokenWithParams extends UsernamePasswordToken {
    private static final long serialVersionUID = -1003682418365507707L;
    private final transient ServletRequest request;

    public UsernamePasswordTokenWithParams(
        String username, String password, boolean rememberMe, String host, ServletRequest request) {
      super(username, password, rememberMe, host);
      this.request = request;
    }

    public String getCleanParam(String paramName) {
      return WebUtils.getCleanParam(request, paramName);
    }
  }
}
