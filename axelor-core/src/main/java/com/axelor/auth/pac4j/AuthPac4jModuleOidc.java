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
import javax.servlet.ServletContext;
import org.pac4j.core.client.Client;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.http.client.direct.HeaderClient;
import org.pac4j.oidc.client.AzureAdClient;
import org.pac4j.oidc.client.GoogleOidcClient;
import org.pac4j.oidc.client.KeycloakOidcClient;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.AzureAdOidcConfiguration;
import org.pac4j.oidc.config.KeycloakOidcConfiguration;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.credentials.authenticator.UserInfoOidcAuthenticator;

public class AuthPac4jModuleOidc extends AuthPac4jModuleForm {

  // Basic configuration
  public static final String CONFIG_OIDC_GENERIC_CLIENT_ID = "auth.oidc.generic.client.id";
  public static final String CONFIG_OIDC_GENERIC_SECRET = "auth.oidc.generic.secret";

  // Additional configuration
  public static final String CONFIG_OIDC_GENERIC_DISCOVERY_URI = "auth.oidc.generic.discovery.uri";
  public static final String CONFIG_OIDC_GENERIC_USE_NONCE = "auth.oidc.use.generic.nonce";
  public static final String CONFIG_OIDC_GENERIC_RESPONSE_TYPE = "auth.oidc.generic.response.type";
  public static final String CONFIG_OIDC_GENERIC_RESPONSE_MODE = "auth.oidc.generic.response.mode";
  public static final String CONFIG_OIDC_GENERIC_SCOPE = "auth.oidc.generic.scope";

  // Direct client configuration
  public static final String CONFIG_OIDC_GENERIC_HEADER_NAME = "auth.oidc.generic.header.name";
  public static final String CONFIG_OIDC_GENERIC_PREFIX_HEADER = "auth.oidc.generic.prefix.header";

  // TODO: add Advanced configuration
  // http://www.pac4j.org/docs/clients/openid-connect.html#3-advanced-configuration

  // Google
  public static final String CONFIG_OIDC_GOOGLE_CLIENT_ID = "auth.oidc.google.client.id";
  public static final String CONFIG_OIDC_GOOGLE_SECRET = "auth.oidc.google.secret";

  // Azure AD
  public static final String CONFIG_OIDC_AZUREAD_CLIENT_ID = "auth.oidc.azuread.client.id";
  public static final String CONFIG_OIDC_AZUREAD_SECRET = "auth.oidc.azuread.secret";
  public static final String CONFIG_OIDC_AZUREAD_TENANT = "auth.oidc.azuread.tenant";

  // Keycloak
  public static final String CONFIG_OIDC_KEYCLOAK_CLIENT_ID = "auth.oidc.keycloak.client.id";
  public static final String CONFIG_OIDC_KEYCLOAK_SECRET = "auth.oidc.keycloak.secret";
  public static final String CONFIG_OIDC_KEYCLOAK_REALM = "auth.oidc.keycloak.realm";
  public static final String CONFIG_OIDC_KEYCLOAK_BASE_URI = "auth.oidc.keycloak.base.uri";

  public AuthPac4jModuleOidc(ServletContext servletContext) {
    super(servletContext);
  }

  @Override
  protected void configureCentralClients() {
    final AppSettings settings = AppSettings.get();

    final String genericClientId = settings.get(CONFIG_OIDC_GENERIC_CLIENT_ID, null);
    if (genericClientId != null) {
      final String secret = settings.get(CONFIG_OIDC_GENERIC_SECRET, null);
      final String discoveryURI = settings.get(CONFIG_OIDC_GENERIC_DISCOVERY_URI, null);
      final boolean useNonce = settings.getBoolean(CONFIG_OIDC_GENERIC_USE_NONCE, false);
      final String responseType = settings.get(CONFIG_OIDC_GENERIC_RESPONSE_TYPE, null);
      final String responseMode = settings.get(CONFIG_OIDC_GENERIC_RESPONSE_MODE, null);
      final String scope = settings.get(CONFIG_OIDC_GENERIC_SCOPE, null);
      final String headerName = settings.get(CONFIG_OIDC_GENERIC_HEADER_NAME, null);
      final String prefixHeader = settings.get(CONFIG_OIDC_GENERIC_PREFIX_HEADER, null);

      final OidcConfiguration config = new OidcConfiguration();
      config.setClientId(genericClientId);
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

      final Client<? extends Credentials, ? extends CommonProfile> client;

      if (headerName != null && prefixHeader != null) {
        final UserInfoOidcAuthenticator authenticator = new UserInfoOidcAuthenticator(config);
        client = new HeaderClient(headerName, prefixHeader.trim() + " ", authenticator);
      } else {
        client = new OidcClient<>(config);
      }

      addClient(client);
    }

    final String googleClientId = settings.get(CONFIG_OIDC_GOOGLE_CLIENT_ID, null);
    if (googleClientId != null) {
      final String googleSecret = settings.get(CONFIG_OIDC_GOOGLE_SECRET, null);

      final OidcConfiguration config = new OidcConfiguration();
      config.setClientId(googleClientId);
      config.setSecret(googleSecret);

      final GoogleOidcClient client = new GoogleOidcClient(config);
      addClient(client);
    }

    final String azureAdClientId = settings.get(CONFIG_OIDC_AZUREAD_CLIENT_ID, null);
    if (azureAdClientId != null) {
      final String azureAdSecret = settings.get(CONFIG_OIDC_AZUREAD_SECRET, null);
      final String azureAdTenant = settings.get(CONFIG_OIDC_AZUREAD_TENANT, null);

      final AzureAdOidcConfiguration config = new AzureAdOidcConfiguration();
      config.setClientId(azureAdClientId);
      config.setSecret(azureAdSecret);
      config.setTenant(azureAdTenant);

      final AzureAdClient client = new AzureAdClient(config);
      addClient(client);
    }

    final String keycloakClientId = settings.get(CONFIG_OIDC_KEYCLOAK_CLIENT_ID, null);
    if (keycloakClientId != null) {
      final String keycloakSecret = settings.get(CONFIG_OIDC_KEYCLOAK_SECRET, null);
      final String keycloakRealm = settings.get(CONFIG_OIDC_KEYCLOAK_REALM, null);
      final String keycloakBaseUri = settings.get(CONFIG_OIDC_KEYCLOAK_BASE_URI, null);

      final KeycloakOidcConfiguration config = new KeycloakOidcConfiguration();
      config.setClientId(keycloakClientId);
      config.setSecret(keycloakSecret);
      config.setRealm(keycloakRealm);
      config.setBaseUri(keycloakBaseUri);

      final KeycloakOidcClient client = new KeycloakOidcClient(config);
      addClient(client);
    }
  }

  public static boolean isEnabled() {
    final AppSettings settings = AppSettings.get();
    return settings.get(CONFIG_OIDC_GENERIC_CLIENT_ID, null) != null
        || settings.get(CONFIG_OIDC_GOOGLE_CLIENT_ID, null) != null
        || settings.get(CONFIG_OIDC_AZUREAD_CLIENT_ID, null) != null
        || settings.get(CONFIG_OIDC_KEYCLOAK_CLIENT_ID, null) != null;
  }
}
