/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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

import com.axelor.inject.Beans;
import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.HttpHeaders;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionException;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.session.mgt.SessionKey;
import org.apache.shiro.web.session.mgt.ServletContainerSessionManager;
import org.apache.shiro.web.util.WebUtils;

/**
 * Session Manager
 *
 * <p>Lets the Servlet container manage the sessions and uses SameSite attribute for secure
 * requests.
 */
public class AxelorSessionManager extends ServletContainerSessionManager {

  protected static final String COOKIE_ATTR_SEPARATOR = "; ";

  @Override
  public Session start(SessionContext context) throws AuthorizationException {
    final HttpServletRequest request = WebUtils.getHttpRequest(context);
    return createSession(
        context, request, Optional.ofNullable(context.getHost()).orElseGet(request::getRemoteHost));
  }

  @Override
  public Session getSession(SessionKey key) throws SessionException {
    final HttpServletRequest request = WebUtils.getHttpRequest(key);
    return createSession(key, request, request.getRemoteHost());
  }

  public void changeSessionId() {
    final HttpServletRequest request = Beans.get(HttpServletRequest.class);
    request.changeSessionId();
    if (AuthPac4jModule.isSecure(request)) {
      setSameSiteNone(Beans.get(HttpServletResponse.class));
    }
  }

  protected Session createSession(Object source, HttpServletRequest request, String host) {
    final HttpSession session = request.getSession();

    if (session.isNew() && AuthPac4jModule.isSecure(request)) {
      final HttpServletResponse response = WebUtils.getHttpResponse(source);
      setSameSiteNone(response);
    }

    return createSession(session, host);
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
