/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.pac4j.core.config.Config;
import org.pac4j.jee.filter.CallbackFilter;

@Singleton
public class AxelorCallbackFilter extends CallbackFilter {

  @Inject
  public AxelorCallbackFilter(Config config, ClientListService clientListService) {
    setConfig(config);
    setDefaultClient(clientListService.getDefaultClientName());
    setRenewSession(false);
  }

  /**
   * Applies session info after the callback processing, as the session does not exist yet before
   * the user is logged in.
   */
  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {
    try {
      super.doFilter(servletRequest, servletResponse, filterChain);
    } finally {
      if (servletRequest instanceof HttpServletRequest httpRequest) {
        SessionInfoFilter.applySessionInfo(httpRequest);
      }
    }
  }
}
