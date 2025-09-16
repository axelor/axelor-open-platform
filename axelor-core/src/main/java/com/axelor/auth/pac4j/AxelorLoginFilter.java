/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import com.axelor.auth.AuthUtils;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.pac4j.jee.filter.SecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AxelorLoginFilter implements Filter {

  private final SecurityFilter securityFilter;
  private final AuthPac4jInfo info;

  private static final Logger logger = LoggerFactory.getLogger(AxelorLoginFilter.class);

  @Inject
  public AxelorLoginFilter(AxelorSecurityFilter securityFilter, AuthPac4jInfo info) {
    this.securityFilter = securityFilter;
    this.info = info;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    final Subject subject = SecurityUtils.getSubject();
    boolean authenticated = subject.isAuthenticated();

    if (authenticated) {
      if (AuthUtils.getUser() == null) {
        logger.warn("Authenticated, but no user: {}", subject.getPrincipal());
        subject.logout();
        authenticated = false;
      } else if (info.getUserProfile(request, response).isEmpty()) {
        logger.warn("Authenticated, but no user profile: {}", subject.getPrincipal());
        subject.logout();
        authenticated = false;
      }
    }

    // If already authenticated, redirect to base URL.
    if (authenticated) {
      ((HttpServletResponse) response).sendRedirect(".");
      return;
    }

    // When not authenticated, this triggers login process.
    securityFilter.doFilter(request, response, chain);
  }
}
