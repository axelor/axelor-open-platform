/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import com.axelor.auth.AuthSessionService;
import jakarta.inject.Singleton;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.IOException;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.servlet.OncePerRequestFilter;

/**
 * Captures request metadata into the current session.
 *
 * <p>On each request, this filter stores contextual information from the HTTP request (such as
 * client IP address and user agent) as session attributes. Attributes are only written when their
 * values have actually changed, to avoid unnecessary session persistence overhead.
 *
 * <p>This filter is intended to be chained before security filters in the Shiro filter chain
 * configuration, so that session metadata is available for all authenticated paths.
 */
@Singleton
public class SessionInfoFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws ServletException, IOException {
    if (servletRequest instanceof HttpServletRequest httpRequest) {
      applySessionInfo(httpRequest);
    }

    filterChain.doFilter(servletRequest, servletResponse);
  }

  /**
   * Sets remote IP and user agent on the current subject's session, only if the subject is
   * authenticated and the values have changed.
   */
  static void applySessionInfo(HttpServletRequest request) {
    Subject subject = SecurityUtils.getSubject();
    if (!subject.isAuthenticated()) {
      return;
    }
    Session session = subject.getSession(false);
    if (session == null) {
      return;
    }
    String ip = request.getRemoteAddr();
    if (!ip.equals(session.getAttribute(AuthSessionService.REMOTE_IP))) {
      session.setAttribute(AuthSessionService.REMOTE_IP, ip);
    }
    String ua = request.getHeader(HttpHeaders.USER_AGENT);
    if (ua != null && !ua.equals(session.getAttribute(AuthSessionService.USER_AGENT))) {
      session.setAttribute(AuthSessionService.USER_AGENT, ua);
    }
  }
}
