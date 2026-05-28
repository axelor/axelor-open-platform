/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import com.google.inject.Provider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.pac4j.core.client.Clients;

@Singleton
public class ClientsProvider implements Provider<Clients> {

  private Clients clients;

  @Inject
  public ClientsProvider(AuthPac4jInfo authPac4jInfo, ClientListService clientListService) {
    this.clients = new Clients("/callback", clientListService.get());
  }

  @Override
  public Clients get() {
    return clients;
  }
}
