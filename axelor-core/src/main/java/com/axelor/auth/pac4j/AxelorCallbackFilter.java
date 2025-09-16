/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
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
}
