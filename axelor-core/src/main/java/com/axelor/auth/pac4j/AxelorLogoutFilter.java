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

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.AuthUtils;
import io.buji.pac4j.filter.LogoutFilter;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.shiro.subject.Subject;
import org.pac4j.core.config.Config;

@Singleton
public class AxelorLogoutFilter extends LogoutFilter {

  private AuthPac4jInfo authPac4jInfo;

  @Inject
  public AxelorLogoutFilter(
      Config config, AxelorLogoutLogic logoutLogic, AuthPac4jInfo authPac4jInfo) {
    final AppSettings settings = AppSettings.get();
    final String logoutUrlPattern =
        settings.get(AvailableAppSettings.AUTH_LOGOUT_URL_PATTERN, null);
    final boolean localLogout = settings.getBoolean(AvailableAppSettings.AUTH_LOGOUT_LOCAL, true);
    final boolean centralLogout =
        settings.getBoolean(AvailableAppSettings.AUTH_LOGOUT_CENTRAL, false);

    this.authPac4jInfo = authPac4jInfo;
    setConfig(config);
    setLogoutUrlPattern(logoutUrlPattern);
    setLocalLogout(localLogout);
    setCentralLogout(centralLogout);
    setLogoutLogic(logoutLogic);
  }

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {

    setDefaultUrl(authPac4jInfo.getLogoutUrl());

    super.doFilter(servletRequest, servletResponse, filterChain);

    if (!Boolean.FALSE.equals(getLocalLogout())) {
      // Log out subject.
      Optional.ofNullable(AuthUtils.getSubject()).ifPresent(Subject::logout);

      // Destroy web session.
      final HttpSession session = ((HttpServletRequest) servletRequest).getSession(false);
      if (session != null) {
        session.invalidate();
      }
    }
  }
}
