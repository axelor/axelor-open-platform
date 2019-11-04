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
import com.axelor.app.AvailableAppSettings;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;

public class AuthPac4jModuleSaml extends AuthPac4jModule {

  private static final Map<String, String> authnRequestBindingTypes =
      ImmutableMap.of(
          "SAML2_POST_BINDING_URI",
          SAMLConstants.SAML2_POST_BINDING_URI,
          "SAML2_POST_SIMPLE_SIGN_BINDING_URI",
          SAMLConstants.SAML2_POST_SIMPLE_SIGN_BINDING_URI,
          "SAML2_REDIRECT_BINDING_URI",
          SAMLConstants.SAML2_REDIRECT_BINDING_URI);

  private static final Map<String, String> responseBindingTypes =
      ImmutableMap.of(
          "SAML2_POST_BINDING_URI",
          SAMLConstants.SAML2_POST_BINDING_URI,
          "SAML2_ARTIFACT_BINDING_URI",
          SAMLConstants.SAML2_ARTIFACT_BINDING_URI);

  public AuthPac4jModuleSaml(ServletContext servletContext) {
    super(servletContext);
    AuthPac4jModule.requireAbsCallbackUrl();
  }

  @Override
  protected void configureClients() {
    final AppSettings settings = AppSettings.get();

    final String keystorePath = settings.get(AvailableAppSettings.AUTH_SAML_KEYSTORE_PATH);
    final String keystorePassword = settings.get(AvailableAppSettings.AUTH_SAML_KEYSTORE_PASSWORD);
    final String privateKeyPassword =
        settings.get(AvailableAppSettings.AUTH_SAML_PRIVATE_KEY_PASSWORD);
    final String identityProviderMetadataPath =
        settings.get(AvailableAppSettings.AUTH_SAML_IDENTITY_PROVIDER_METADATA_PATH);

    final int maximumAuthenticationLifetime =
        settings.getInt(AvailableAppSettings.AUTH_SAML_MAXIMUM_AUTHENTICATION_LIFETIME, -1);
    final String serviceProviderEntityId =
        settings.get(AvailableAppSettings.AUTH_SAML_SERVICE_PROVIDER_ENTITY_ID, null);
    final String serviceProviderMetadataPath =
        settings.get(AvailableAppSettings.AUTH_SAML_SERVICE_PROVIDER_METADATA_PATH, null);

    final boolean forceAuth = settings.getBoolean(AvailableAppSettings.AUTH_SAML_FORCE_AUTH, false);
    final boolean passive = settings.getBoolean(AvailableAppSettings.AUTH_SAML_PASSIVE, false);

    final String authnRequestBindingType =
        settings.get(AvailableAppSettings.AUTH_SAML_AUTHN_REQUEST_BINDING_TYPE, null);
    final String responseBindingType =
        settings.get(AvailableAppSettings.AUTH_SAML_RESPONSE_BINDING_TYPE, null);

    final boolean useNameQualifier =
        settings.getBoolean(AvailableAppSettings.AUTH_SAML_USE_NAME_QUALIFIER, false);

    final int attributeConsumingServiceIndex =
        settings.getInt(AvailableAppSettings.AUTH_SAML_ATTRIBUTE_CONSUMING_SERVICE_INDEX, -1);
    final int assertionConsumerServiceIndex =
        settings.getInt(AvailableAppSettings.AUTH_SAML_ASSERTION_CONSUMER_SERVICE_INDEX, -1);

    final List<String> blackListedSignatureSigningAlgorithms =
        settings.getList(AvailableAppSettings.AUTH_SAML_BLACKLISTED_SIGNATURE_SIGNING_ALGORITHMS);
    final List<String> signatureAlgorithms =
        settings.getList(AvailableAppSettings.AUTH_SAML_SIGNATURE_ALGORITHMS);
    final List<String> signatureReferenceDigestMethods =
        settings.getList(AvailableAppSettings.AUTH_SAML_SIGNATURE_REFERENCE_DIGEST_METHODS);
    final String signatureCanonicalizationAlgorithm =
        settings.get(AvailableAppSettings.AUTH_SAML_SIGNATURE_CANONICALIZATION_ALGORITHM, null);

    final boolean wantsAssertionsSigned =
        settings.getBoolean(AvailableAppSettings.AUTH_SAML_WANTS_ASSERTIONS_SIGNED, false);
    final boolean authnRequestSigned =
        settings.getBoolean(AvailableAppSettings.AUTH_SAML_AUTHN_REQUEST_SIGNED, false);

    final SAML2Configuration saml2Config =
        new SAML2Configuration(
            keystorePath, keystorePassword, privateKeyPassword, identityProviderMetadataPath);

    if (maximumAuthenticationLifetime >= 0) {
      saml2Config.setMaximumAuthenticationLifetime(maximumAuthenticationLifetime);
    }

    if (serviceProviderEntityId != null) {
      saml2Config.setServiceProviderEntityId(serviceProviderEntityId);
    }
    if (serviceProviderMetadataPath != null) {
      saml2Config.setServiceProviderMetadataPath(serviceProviderMetadataPath);
    }

    saml2Config.setForceAuth(forceAuth);
    saml2Config.setPassive(passive);

    if (authnRequestBindingType != null) {
      saml2Config.setAuthnRequestBindingType(
          authnRequestBindingTypes.getOrDefault(authnRequestBindingType, authnRequestBindingType));
    }

    if (responseBindingType != null) {
      saml2Config.setResponseBindingType(
          responseBindingTypes.getOrDefault(responseBindingType, responseBindingType));
    }

    saml2Config.setUseNameQualifier(useNameQualifier);

    if (attributeConsumingServiceIndex >= 0) {
      saml2Config.setAttributeConsumingServiceIndex(attributeConsumingServiceIndex);
    }
    if (assertionConsumerServiceIndex >= 0) {
      saml2Config.setAssertionConsumerServiceIndex(assertionConsumerServiceIndex);
    }

    if (!blackListedSignatureSigningAlgorithms.isEmpty()) {
      saml2Config.setBlackListedSignatureSigningAlgorithms(blackListedSignatureSigningAlgorithms);
    }
    if (!signatureAlgorithms.isEmpty()) {
      saml2Config.setSignatureAlgorithms(signatureAlgorithms);
    }
    if (!signatureReferenceDigestMethods.isEmpty()) {
      saml2Config.setSignatureReferenceDigestMethods(signatureReferenceDigestMethods);
    }
    if (signatureCanonicalizationAlgorithm != null) {
      saml2Config.setSignatureCanonicalizationAlgorithm(signatureCanonicalizationAlgorithm);
    }

    saml2Config.setWantsAssertionsSigned(wantsAssertionsSigned);
    saml2Config.setAuthnRequestSigned(authnRequestSigned);

    final SAML2Client client = new AxelorSAML2Client(saml2Config);
    addCentralClient(client);
  }

  public static boolean isEnabled() {
    final AppSettings settings = AppSettings.get();
    return settings.get(AvailableAppSettings.AUTH_SAML_KEYSTORE_PATH, null) != null;
  }

  private static class AxelorSAML2Client extends SAML2Client {
    public AxelorSAML2Client(SAML2Configuration configuration) {
      super(configuration);
      setName(getClass().getSuperclass().getSimpleName());
    }

    @Override
    protected void clientInit() {
      // Default service provider entity ID to "callback URL" + "?client_name=SAML2Client"
      if (configuration.getServiceProviderEntityId() == null) {
        final String serviceProviderEntityId =
            String.format("%s?client_name=%s", AuthPac4jModule.getCallbackUrl(), getName());
        configuration.setServiceProviderEntityId(serviceProviderEntityId);
      }

      // Post Logout URL
      configuration.setPostLogoutURL(AuthPac4jModule.getRelativeBaseURL());
      super.clientInit();
    }
  }
}
