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
package com.axelor.auth.pac4j;

import com.axelor.common.StringUtils;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.function.Function;
import javax.servlet.ServletContext;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.Client;
import org.pac4j.http.client.direct.HeaderClient;
import org.pac4j.oidc.client.AzureAdClient;
import org.pac4j.oidc.client.GoogleOidcClient;
import org.pac4j.oidc.client.KeycloakOidcClient;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.AzureAdOidcConfiguration;
import org.pac4j.oidc.config.KeycloakOidcConfiguration;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.credentials.authenticator.UserInfoOidcAuthenticator;

public class AuthPac4jModuleOidc extends AuthPac4jModuleMultiClient {

  private static final Map<String, Map<String, String>> allSettings = getAuthSettings("auth.oidc.");

  private final Map<String, Function<Map<String, String>, Client<?, ?>>> providers =
      ImmutableMap.<String, Function<Map<String, String>, Client<?, ?>>>builder()
          .put("google", this::setupGoogle)
          .put("azuread", this::setupAzureAd)
          .put("keycloak", this::setupKeycloak)
          .build();

  public AuthPac4jModuleOidc(ServletContext servletContext) {
    super(servletContext);
  }

  @Override
  protected void configureClients() {
    addFormClientIfNotExclusive(allSettings);
    addCentralClients(allSettings, providers);
  }

  @Override
  protected Client<?, ?> setupGeneric(Map<String, String> settings, String providerName) {
    final String clientId = settings.get("client.id");
    final String secret = settings.get("secret");
    final String discoveryURI = settings.get("discovery.uri");
    final boolean useNonce = settings.getOrDefault("use.nonce", "false").equals("true");
    final String responseType = settings.get("response.type");
    final String responseMode = settings.get("response.mode");
    final String scope = settings.get("scope");
    final String headerName = settings.get("header.name");
    final String prefixHeader = settings.get("prefix.header");

    final String name = settings.getOrDefault("name", providerName);
    final String title = settings.getOrDefault("title", "OpenID Connect");
    final String icon = settings.getOrDefault("icon", "img/signin/openid.svg");

    final OidcConfiguration config = new OidcConfiguration();
    config.setClientId(clientId);
    config.setSecret(secret);
    config.setUseNonce(useNonce);

    if (StringUtils.notBlank(discoveryURI)) {
      config.setDiscoveryURI(discoveryURI);
    }

    if (StringUtils.notBlank(responseType)) {
      config.setResponseType(responseType);
    }

    if (StringUtils.notBlank(responseMode)) {
      config.setResponseMode(responseMode);
    }

    if (StringUtils.notBlank(scope)) {
      config.setScope(scope);
    }

    final BaseClient<?, ?> client;

    if (StringUtils.notBlank(headerName) && StringUtils.notBlank(prefixHeader)) {
      final UserInfoOidcAuthenticator authenticator = new UserInfoOidcAuthenticator(config);
      client = new HeaderClient(headerName, prefixHeader.trim() + " ", authenticator);
    } else {
      client = new OidcClient<>(config);
    }

    client.setName(name);
    setClientInfo(client.getName(), ImmutableMap.of("title", title, "icon", icon));
    return client;
  }

  private Client<?, ?> setupGoogle(Map<String, String> settings) {
    final String clientId = settings.get("client.id");
    final String secret = settings.get("secret");

    final String title = settings.getOrDefault("title", "Google");
    final String icon = settings.getOrDefault("icon", "img/signin/google.svg");

    final OidcConfiguration config = new OidcConfiguration();
    config.setClientId(clientId);
    config.setSecret(secret);

    final GoogleOidcClient client = new GoogleOidcClient(config);
    setClientInfo(client.getName(), ImmutableMap.of("title", title, "icon", icon));
    return client;
  }

  private Client<?, ?> setupAzureAd(Map<String, String> settings) {
    final String clientId = settings.get("client.id");
    final String secret = settings.get("secret");
    final String tenant = settings.get("tenant");

    final String title = settings.getOrDefault("title", "Azure Active Directory");
    final String icon = settings.getOrDefault("icon", "img/signin/microsoft.svg");

    final AzureAdOidcConfiguration config = new AzureAdOidcConfiguration();
    config.setClientId(clientId);
    config.setSecret(secret);
    config.setTenant(tenant);

    final AzureAdClient client = new AzureAdClient(config);
    setClientInfo(client.getName(), ImmutableMap.of("title", title, "icon", icon));
    return client;
  }

  private Client<?, ?> setupKeycloak(Map<String, String> settings) {
    final String clientId = settings.get("client.id");
    final String secret = settings.get("secret");
    final String realm = settings.get("realm");
    final String baseUri = settings.get("base.uri");

    final String title = settings.getOrDefault("title", "Keycloak");
    final String icon = settings.getOrDefault("icon", "img/signin/keycloak.svg");

    final KeycloakOidcConfiguration config = new KeycloakOidcConfiguration();
    config.setClientId(clientId);
    config.setSecret(secret);
    config.setRealm(realm);
    config.setBaseUri(baseUri);

    final KeycloakOidcClient client = new KeycloakOidcClient(config);
    setClientInfo(client.getName(), ImmutableMap.of("title", title, "icon", icon));
    return client;
  }

  public static boolean isEnabled() {
    return !allSettings.isEmpty();
  }
}
