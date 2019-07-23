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
      ImmutableMap.<String, Entry<String, String>>builder()
          // OpenID Connect
          .put("OidcClient", new SimpleImmutableEntry<>("OpenID Connect", "openid.png"))
          .put("HeaderClient", new SimpleImmutableEntry<>("OpenID Connect", "openid.png"))
          .put("GoogleOidcClient", new SimpleImmutableEntry<>("Google", "google.svg"))
          .put("AzureAdClient", new SimpleImmutableEntry<>("Azure AD", "microsoft.svg"))
          .put("KeycloakOidcClient", new SimpleImmutableEntry<>("Keycloak", "keycloak.svg"))

          // OAuth
          .put("GenericOAuth20Client", new SimpleImmutableEntry<>("OAuth 2.0", "oauth2.png"))
          .put("Google2Client", new SimpleImmutableEntry<>("Google", "google.svg"))
          .put("FacebookClient", new SimpleImmutableEntry<>("Facebook", "facebook.svg"))
          .put("TwitterClient", new SimpleImmutableEntry<>("Twitter", "twitter.svg"))
          .put("YahooClient", new SimpleImmutableEntry<>("Yahoo!", "yahoo.svg"))
          .put("LinkedIn2Client", new SimpleImmutableEntry<>("LinkedIn", "linkedin.svg"))
          .put("WindowsLiveClient", new SimpleImmutableEntry<>("Windows Live", "microsoft.svg"))
          .put("WechatClient", new SimpleImmutableEntry<>("WeChat", "wechat.svg"))
          .put("GitHubClient", new SimpleImmutableEntry<>("GitHub", "github.svg"))
          .build();

  private AuthCentral() {}

  public static Entry<String, String> getInfo(String clientName) {

    return info.get(clientName);
  }
}
