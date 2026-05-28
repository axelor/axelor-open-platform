/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.pac4j.config.LogoutConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletException;
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
}
