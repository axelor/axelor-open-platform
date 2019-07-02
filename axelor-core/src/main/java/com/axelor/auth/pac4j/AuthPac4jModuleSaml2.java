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
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;

public class AuthPac4jModuleSaml2 extends AuthPac4jModule {

  protected static final String CONFIG_SAML2_KEYSTORE_PATH = "saml2.keystore.path";
  protected static final String CONFIG_SAML2_KEYSTORE_PASSWORD = "saml2.keystore.password";
  protected static final String CONFIG_SAML2_PRIVATE_KEY_PASSWORD = "saml2.private.key.password";
  protected static final String CONFIG_SAML2_IDENTITY_PROVIDER_METADATA_PATH =
      "saml2.identity.provider.metadata.path";
  protected static final String CONFIG_SAML2_MAXIMUM_AUTHENTICATION_LIFETIME =
      "saml2.maximum.authentication.lifetime";
  protected static final String CONFIG_SAML2_SERVICE_PROVIDER_ENTITY_ID =
      "saml2.service.provider.entity.id";
  protected static final String CONFIG_SAML2_SERVICE_PROVIDER_METADATA_PATH =
      "saml2.service.provider.metadata.path";

  public AuthPac4jModuleSaml2(ServletContext servletContext) {
    super(servletContext);
  }

  @Override
  protected void configureClients() {
    final AppSettings settings = AppSettings.get();
    final String keystorePath = settings.get(CONFIG_SAML2_KEYSTORE_PATH);
    final String keystorePassword = settings.get(CONFIG_SAML2_KEYSTORE_PASSWORD);
    final String privateKeyPassword = settings.get(CONFIG_SAML2_PRIVATE_KEY_PASSWORD);
    final String identityProviderMetadataPath =
        settings.get(CONFIG_SAML2_IDENTITY_PROVIDER_METADATA_PATH);
    final int maximumAuthenticationLifetime =
        settings.getInt(CONFIG_SAML2_MAXIMUM_AUTHENTICATION_LIFETIME, 0);
    final String serviceProviderEntityId =
        settings.get(CONFIG_SAML2_SERVICE_PROVIDER_ENTITY_ID, null);
    final String serviceProviderMetadataPath =
        settings.get(CONFIG_SAML2_SERVICE_PROVIDER_METADATA_PATH, null);
    final SAML2Configuration saml2Config =
        new SAML2Configuration(
            keystorePath, keystorePassword, privateKeyPassword, identityProviderMetadataPath);

    if (maximumAuthenticationLifetime > 0) {
      saml2Config.setMaximumAuthenticationLifetime(maximumAuthenticationLifetime);
    }

    if (serviceProviderEntityId != null) {
      saml2Config.setServiceProviderEntityId(serviceProviderEntityId);
    }

    if (serviceProviderMetadataPath != null) {
      saml2Config.setServiceProviderMetadataPath(serviceProviderMetadataPath);
    }

    SAML2Client client = new SAML2Client(saml2Config);
    addClient(client);
  }

  public static boolean isEnabled() {
    final AppSettings settings = AppSettings.get();
    return settings.get(CONFIG_SAML2_KEYSTORE_PATH, null) != null;
  }
}
