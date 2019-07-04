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

import com.axelor.app.AppSettings;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.servlet.ServletContext;
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

public class AuthPac4jModuleOidc extends AuthPac4jModule {

  // Basic configuration
  protected static final String CONFIG_OIDC_CLIENT_ID = "auth.oidc.client.id";
  protected static final String CONFIG_OIDC_SECRET = "auth.oidc.secret";
  protected static final String CONFIG_OIDC_PROVIDER = "auth.oidc.provider";

  // Additional configuration
  protected static final String CONFIG_OIDC_DISCOVERY_URI = "auth.oidc.discovery.uri";
  protected static final String CONFIG_OIDC_USE_NONCE = "auth.oidc.use.nonce";
  protected static final String CONFIG_OIDC_RESPONSE_TYPE = "auth.oidc.response.type";
  protected static final String CONFIG_OIDC_RESPONSE_MODE = "auth.oidc.response.mode";
  protected static final String CONFIG_OIDC_SCOPE = "auth.oidc.scope";

  // Keycloak client configuration
  protected static final String CONFIG_OIDC_REALM = "auth.oidc.realm";
  protected static final String CONFIG_OIDC_BASE_URI = "auth.oidc.base.uri";

  // Direct client configuration
  protected static final String CONFIG_OIDC_HEADER_NAME = "auth.oidc.header.name";
  protected static final String CONFIG_OIDC_PREFIX_HEADER = "auth.oidc.prefix.header";

  private static final Map<String, Supplier<OidcClientSupplier>> providers =
      ImmutableMap.of(
          "generic",
          OidcClientSupplier::new,
          "google",
          GoogleOidcClientSupplier::new,
          "azure",
          AzureAdClientSupplier::new,
          "keycloak",
          KeycloakOidcClientSupplier::new,
          "direct",
          DirectClientSupplier::new);

  public AuthPac4jModuleOidc(ServletContext servletContext) {
    super(servletContext);
  }

  @Override
  protected void configureClients() {
    final AppSettings settings = AppSettings.get();
    final String providerName = settings.get(CONFIG_OIDC_PROVIDER, null);
    final OidcClientSupplier supplier =
        providers.getOrDefault(providerName, OidcClientSupplier::new).get();
    addClient(supplier.get());
  }

  public static boolean isEnabled() {
    final AppSettings settings = AppSettings.get();
    return settings.get(CONFIG_OIDC_CLIENT_ID, null) != null;
  }

  @SuppressWarnings("rawtypes")
  private static class OidcClientSupplier implements Supplier<Client> {

    @Override
    public Client get() {
      final OidcConfiguration config = getConfiguration();
      configure(config);
      return getClient(config);
    }

    protected OidcConfiguration getConfiguration() {
      return new OidcConfiguration();
    }

    protected void configure(OidcConfiguration config) {
      final AppSettings settings = AppSettings.get();
      final String clientId = settings.get(CONFIG_OIDC_CLIENT_ID);
      final String secret = settings.get(CONFIG_OIDC_SECRET);

      final String discoveryURI = settings.get(CONFIG_OIDC_DISCOVERY_URI, null);
      final boolean useNonce = settings.getBoolean(CONFIG_OIDC_USE_NONCE, false);
      final String responseType = settings.get(CONFIG_OIDC_RESPONSE_TYPE, null);
      final String responseMode = settings.get(CONFIG_OIDC_RESPONSE_MODE, null);
      final String scope = settings.get(CONFIG_OIDC_SCOPE, null);

      config.setClientId(clientId);
      config.setSecret(secret);
      config.setUseNonce(useNonce);

      if (discoveryURI != null) {
        config.setDiscoveryURI(discoveryURI);
      }

      if (responseType != null) {
        config.setResponseType(responseType);
      }

      if (responseMode != null) {
        config.setResponseMode(responseMode);
      }

      if (scope != null) {
        config.setScope(scope);
      }
    }

    @SuppressWarnings("unchecked")
    protected Client getClient(OidcConfiguration config) {
      return new OidcClient(config);
    }
  }

  private static class GoogleOidcClientSupplier extends OidcClientSupplier {

    @Override
    @SuppressWarnings("rawtypes")
    protected Client getClient(OidcConfiguration config) {
      return new GoogleOidcClient(config);
    }
  }

  private static class AzureAdClientSupplier extends OidcClientSupplier {

    @Override
    @SuppressWarnings("rawtypes")
    protected Client getClient(OidcConfiguration config) {
      return new AzureAdClient(new AzureAdOidcConfiguration(config));
    }
  }

  private static class KeycloakOidcClientSupplier extends OidcClientSupplier {

    @Override
    protected OidcConfiguration getConfiguration() {
      return new KeycloakOidcConfiguration();
    }

    @Override
    protected void configure(OidcConfiguration config) {
      super.configure(config);

      final KeycloakOidcConfiguration keycloakOidcConfig = (KeycloakOidcConfiguration) config;
      final AppSettings settings = AppSettings.get();
      final String realm = settings.get(CONFIG_OIDC_REALM);
      final String baseUri = settings.get(CONFIG_OIDC_BASE_URI);

      keycloakOidcConfig.setRealm(realm);
      keycloakOidcConfig.setBaseUri(baseUri);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Client getClient(OidcConfiguration config) {
      return new KeycloakOidcClient((KeycloakOidcConfiguration) config);
    }
  }

  private static class DirectClientSupplier extends OidcClientSupplier {

    @Override
    @SuppressWarnings("rawtypes")
    protected Client getClient(OidcConfiguration config) {
      final AppSettings settings = AppSettings.get();
      final String headerName = settings.get(CONFIG_OIDC_HEADER_NAME, "Authorization");
      final String prefixHeader = settings.get(CONFIG_OIDC_PREFIX_HEADER, "Bearer ");

      UserInfoOidcAuthenticator authenticator = new UserInfoOidcAuthenticator(config);
      return new HeaderClient(headerName, prefixHeader, authenticator);
    }
  }
}
