/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.auth.pac4j.local;

import com.axelor.auth.pac4j.AxelorCallbackClientFinder;
import java.util.List;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.WebContext;

/**
 * Checks for basic auth request header and finds basic auth client when appropriate. This avoids
 * having to specify request parameter "client_name" when using IndirectBasicAuth.
 */
public class BasicAuthCallbackClientFinder extends AxelorCallbackClientFinder {

  private final String clientName;

  public BasicAuthCallbackClientFinder(String clientName) {
    this.clientName = clientName;
  }

  @Override
  public List<Client> find(Clients clients, WebContext context, String clientNames) {

    if (context
        .getRequestHeader(HttpConstants.AUTHORIZATION_HEADER)
        .map(header -> header.startsWith(HttpConstants.BASIC_HEADER_PREFIX))
        .isPresent()) {
      clientNames = clientName;
    }

    return super.find(clients, context, clientNames);
  }
}
