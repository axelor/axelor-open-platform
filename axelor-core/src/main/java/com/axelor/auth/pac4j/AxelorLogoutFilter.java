/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.pac4j.config.LogoutConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.pac4j.jee.filter.LogoutFilter;

@Singleton
public class AxelorLogoutFilter extends LogoutFilter {

  @Inject
  public AxelorLogoutFilter(LogoutConfig config, AxelorLogoutFilterConfig filterConfig)
      throws ServletException {

    final AppSettings settings = AppSettings.get();
    final String logoutUrlPattern =
        settings.get(AvailableAppSettings.AUTH_LOGOUT_URL_PATTERN, null);

    setConfig(config);
    setLogoutUrlPattern(logoutUrlPattern);

    init(filterConfig);
  }

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {

    super.doFilter(servletRequest, servletResponse, filterChain);

    if (!Boolean.FALSE.equals(getLocalLogout())) {
      // Log out subject.
      Optional.ofNullable(AuthUtils.getSubject()).ifPresent(Subject::logout);

      // Destroy web session.
      final Session session = SecurityUtils.getSubject().getSession(false);
      if (session != null) {
        session.stop();
      }
    }
  }
}
