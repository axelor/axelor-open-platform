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

import com.axelor.auth.AuthUtils;
import io.buji.pac4j.profile.ShiroProfileManager;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.pac4j.core.client.Clients;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.jee.context.JEEContext;
import org.pac4j.jee.context.session.JEESessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AxelorLoginPageFilter implements Filter {

  private final AxelorCallbackFilter axelorCallbackFilter;

  private final Clients clients;

  private static final Logger logger = LoggerFactory.getLogger(AxelorLoginPageFilter.class);

  @Inject
  public AxelorLoginPageFilter(AxelorCallbackFilter axelorCallbackFilter, Clients clients) {
    this.axelorCallbackFilter = axelorCallbackFilter;
    this.clients = clients;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    final Subject subject = SecurityUtils.getSubject();
    final boolean authenticated = subject.isAuthenticated();

    if (authenticated) {
      if (AuthUtils.getUser() == null) {
        logger.warn("Authenticated, but no user: {}", subject.getPrincipal());
        subject.logout();
      } else if (getUserProfile(request, response).isEmpty()) {
        logger.warn("Authenticated, but no user profile: {}", subject.getPrincipal());
        subject.logout();
      }
    }

    // if already authenticated or if form login is not configured redirect to base url
    if (authenticated || clients.getClients().stream().noneMatch(FormClient.class::isInstance)) {
      ((HttpServletResponse) response).sendRedirect(".");
      return;
    }

    // Perform callback filter if request body is not empty.
    if (request.getContentLengthLong() > 0L) {
      axelorCallbackFilter.doFilter(request, response, chain);
      return;
    }

    chain.doFilter(request, response);
  }

  private Optional<UserProfile> getUserProfile(ServletRequest request, ServletResponse response) {
    final JEEContext context =
        new JEEContext((HttpServletRequest) request, (HttpServletResponse) response);
    final ShiroProfileManager profileManager =
        new ShiroProfileManager(context, JEESessionStore.INSTANCE);

    return profileManager.getProfile();
  }

  @Override
  public void init(javax.servlet.FilterConfig filterConfig) throws ServletException {
    // Do nothing
  }

  @Override
  public void destroy() {
    // Do nothing
  }
}
