/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.stream.Collectors;
import org.pac4j.core.client.Client;
import org.pac4j.core.config.Config;
import org.pac4j.jee.filter.SecurityFilter;

@Singleton
public class AxelorSecurityFilter extends SecurityFilter {

  @Inject
  public AxelorSecurityFilter(Config config) {
    setConfig(config);
    setClients(
        config.getClients().getClients().stream()
            .map(Client::getName)
            .collect(Collectors.joining(",")));
  }
}
