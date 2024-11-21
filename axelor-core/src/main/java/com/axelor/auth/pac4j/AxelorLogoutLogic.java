/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.FrameworkParameters;
import org.pac4j.core.engine.DefaultLogoutLogic;

@Singleton
public class AxelorLogoutLogic extends DefaultLogoutLogic {

  private final AuthPac4jInfo info;

  @Inject
  public AxelorLogoutLogic(AuthPac4jInfo info) {
    this.info = info;
  }

  @Override
  public Object perform(
      Config config,
      String defaultUrl,
      String inputLogoutUrlPattern,
      Boolean inputLocalLogout,
      Boolean inputDestroySession,
      Boolean inputCentralLogout,
      FrameworkParameters parameters) {

    return super.perform(
        config,
        info.getLogoutUrl(),
        inputLogoutUrlPattern,
        inputLocalLogout,
        inputDestroySession,
        inputCentralLogout,
        parameters);
  }
}
