/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.pac4j.http.client.indirect.FormClient;

@Singleton
public class ClientListDefaultService extends ClientListService {

  @Inject
  public ClientListDefaultService(FormClient formClient) {
    clients.add(formClient);
    defaultClientName = formClient.getName();
    init();
  }
}
