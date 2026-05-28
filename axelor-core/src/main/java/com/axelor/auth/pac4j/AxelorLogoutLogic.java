/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
