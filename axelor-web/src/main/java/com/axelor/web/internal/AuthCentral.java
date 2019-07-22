/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.web.internal;

import com.google.common.collect.ImmutableMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Map.Entry;

public class AuthCentral {

  private static final Map<String, Entry<String, String>> info =
      ImmutableMap.of(
          "OidcClient",
          new SimpleImmutableEntry<>("OpenID Connect", "openid.png"),
          "HeaderClient",
          new SimpleImmutableEntry<>("OpenID Connect", "openid.png"),
          "GoogleOidcClient",
          new SimpleImmutableEntry<>("Google", "google.svg"),
          "AzureAdClient",
          new SimpleImmutableEntry<>("Azure AD", "microsoft.svg"),
          "KeycloakOidcClient",
          new SimpleImmutableEntry<>("Keycloak", "keycloak.svg"));

  private AuthCentral() {}

  public static Entry<String, String> getInfo(String clientName) {
    return info.get(clientName);
  }
}
