/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.HttpHeaders;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionException;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.session.mgt.SessionKey;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.apache.shiro.web.session.mgt.ServletContainerSessionManager;
import org.apache.shiro.web.util.WebUtils;

/**
 * Session Manager
 *
 * <p>Lets the Servlet container manage the sessions and uses SameSite attribute for secure
 * requests.
 */
@Singleton
public class AxelorSessionManager extends ServletContainerSessionManager {

  protected static final String COOKIE_ATTR_SEPARATOR = "; ";
  private static final Pattern COOKIE_PATH_PATTERN = Pattern.compile("(Path|path)=(.*?);");

  @Override
  public Session start(SessionContext context) throws AuthorizationException {
    final HttpServletRequest request = WebUtils.getHttpRequest(context);
    return getSession(
        true,
        context,
        request,
        Optional.ofNullable(context.getHost()).orElseGet(request::getRemoteHost));
  }

  @Override
  public Session getSession(SessionKey key) throws SessionException {
    final HttpServletRequest request = WebUtils.getHttpRequest(key);
    return getSession(false, key, request, request.getRemoteHost());
  }

  public void changeSessionId() {
    final Subject subject = SecurityUtils.getSubject();
    final HttpServletRequest request = WebUtils.getHttpRequest(subject);
    final HttpServletResponse response = WebUtils.getHttpResponse(subject);
    final boolean sessionCreationEnabled =
        !Boolean.FALSE.equals(request.getAttribute(DefaultSubjectContext.SESSION_CREATION_ENABLED));

    if (subject.getSession(sessionCreationEnabled) != null) {
      request.changeSessionId();
    }

    if (request.isSecure()) {
      setSameSiteNone(response);
    }

    updateCookiePath(response, request.getContextPath());
  }

  protected Session getSession(
      boolean create, Object source, HttpServletRequest request, String host) {
    final HttpSession httpSession = request.getSession(create);

    if (httpSession == null) {
      return null;
    }

    final HttpServletResponse response = WebUtils.getHttpResponse(source);

    if (httpSession.isNew() && request.isSecure()) {
      setSameSiteNone(response);
    }

    Session session = createSession(httpSession, host);

    updateCookiePath(response, request.getContextPath());

    return session;
  }

  private void updateCookiePath(HttpServletResponse httpResponse, String contextPath) {
    final Iterator<String> it = httpResponse.getHeaders(HttpHeaders.SET_COOKIE).iterator();

    if (it.hasNext()) {
      httpResponse.setHeader(HttpHeaders.SET_COOKIE, updateCookiePath(it.next(), contextPath));
      while (it.hasNext()) {
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, updateCookiePath(it.next(), contextPath));
      }
    }
  }

  private String updateCookiePath(String header, String contextPath) {
    if (contextPath.length() == 0) {
      contextPath = "/";
    }
    return COOKIE_PATH_PATTERN.matcher(header).replaceAll("$1=" + contextPath + ";");
  }

  protected void setSameSiteNone(HttpServletResponse response) {
    final Iterator<String> it = response.getHeaders(HttpHeaders.SET_COOKIE).iterator();

    if (it.hasNext()) {
      addSameSiteCookieHeader(response::setHeader, it.next());
      while (it.hasNext()) {
        addSameSiteCookieHeader(response::addHeader, it.next());
      }
    }
  }

  protected void addSameSiteCookieHeader(BiConsumer<String, String> adder, String header) {
    final List<String> parts = Lists.newArrayList(header, "SameSite=None");

    if (header.indexOf(COOKIE_ATTR_SEPARATOR + "Secure") < 0) {
      parts.add("Secure");
    }

    adder.accept(
        HttpHeaders.SET_COOKIE, parts.stream().collect(Collectors.joining(COOKIE_ATTR_SEPARATOR)));
  }
}
