/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
package com.axelor.auth.pac4j.ldap;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.pac4j.AuthPac4jProfileService;
import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.ldaptive.BindConnectionInitializer;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.Credential;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.PooledConnectionFactory;
import org.ldaptive.SearchOperation;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchResponse;
import org.ldaptive.ad.handler.ObjectGuidHandler;
import org.ldaptive.ad.handler.ObjectSidHandler;
import org.ldaptive.auth.Authenticator;
import org.ldaptive.auth.DnResolver;
import org.ldaptive.auth.FormatDnResolver;
import org.ldaptive.auth.SearchDnResolver;
import org.ldaptive.auth.SimpleBindAuthenticationHandler;
import org.ldaptive.sasl.Mechanism;
import org.ldaptive.sasl.SaslConfig;
import org.ldaptive.ssl.CredentialConfig;
import org.ldaptive.ssl.KeyStoreCredentialConfig;
import org.ldaptive.ssl.SslConfig;
import org.ldaptive.ssl.X509CredentialConfig;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.exception.BadCredentialsException;
import org.pac4j.ldap.profile.LdapProfile;
import org.pac4j.ldap.profile.service.LdapProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AxelorLdapProfileService extends LdapProfileService {

  private final String groupsDn;
  private final String groupFilter;

  protected static final String FILTER_FORMAT = "(%s=%s)";

  protected final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Inject
  public AxelorLdapProfileService() {
    this(AppSettings.get().getProperties());
  }

  public AxelorLdapProfileService(Map<String, String> properties) {
    final String ldapUrl = properties.get(AvailableAppSettings.AUTH_LDAP_SERVER_URL);
    final String usersDn = properties.get(AvailableAppSettings.AUTH_LDAP_USER_BASE);
    final String idAttribute =
        properties.getOrDefault(
            AvailableAppSettings.AUTH_LDAP_USER_ID_ATTRIBUTE, AxelorLdapProfileDefinition.USERNAME);
    final String usernameAttribute =
        properties.getOrDefault(
            AvailableAppSettings.AUTH_LDAP_USER_USERNAME_ATTRIBUTE,
            AxelorLdapProfileDefinition.USERNAME);
    final String userFilter =
        Optional.ofNullable(properties.get(AvailableAppSettings.AUTH_LDAP_USER_FILTER))
            .map(property -> property.replace("{0}", "{user}"))
            .orElse(null);
    final String userDnFormat =
        properties.getOrDefault(AvailableAppSettings.AUTH_LDAP_USER_DN_FORMAT, null);
    final String systemDn = properties.get(AvailableAppSettings.AUTH_LDAP_SERVER_AUTH_USER);
    final String systemPassword =
        properties.get(AvailableAppSettings.AUTH_LDAP_SERVER_AUTH_PASSWORD);
    final String authenticationType =
        properties.get(AvailableAppSettings.AUTH_LDAP_SERVER_AUTH_TYPE);
    final boolean useStartTLS =
        Boolean.parseBoolean(properties.get(AvailableAppSettings.AUTH_LDAP_SERVER_STARTTLS));
    final String trustStore =
        properties.get(AvailableAppSettings.AUTH_LDAP_SERVER_SSL_TRUST_STORE_PATH);
    final String keyStore =
        properties.get(AvailableAppSettings.AUTH_LDAP_SERVER_SSL_KEY_STORE_PATH);
    final String trustCertificates =
        properties.get(AvailableAppSettings.AUTH_LDAP_SERVER_SSL_CERT_TRUST_PATH);
    final Duration connectTimeout =
        Optional.ofNullable(properties.get(AvailableAppSettings.AUTH_LDAP_SERVER_CONNECT_TIMEOUT))
            .filter(StringUtils::notBlank)
            .map(Long::parseLong)
            .map(Duration::ofSeconds)
            .orElse(null);
    final Duration responseTimeout =
        Optional.ofNullable(properties.get(AvailableAppSettings.AUTH_LDAP_SERVER_RESPONSE_TIMEOUT))
            .filter(StringUtils::notBlank)
            .map(Long::parseLong)
            .map(Duration::ofSeconds)
            .orElse(null);

    final SaslConfig saslConfig = getSaslConfig(authenticationType);
    final SslConfig sslConfig;
    final CredentialConfig credentialConfig;

    if (StringUtils.notBlank(trustStore)) {
      final String trustStorePassword =
          properties.get(AvailableAppSettings.AUTH_LDAP_SERVER_SSL_TRUST_STORE_PASSWORD);
      final String trustStoreType =
          properties.get(AvailableAppSettings.AUTH_LDAP_SERVER_SSL_TRUST_STORE_TYPE);
      final String[] trustStoreAliases =
          Optional.ofNullable(
                  properties.get(AvailableAppSettings.AUTH_LDAP_SERVER_SSL_TRUST_STORE_ALIASES))
              .map(storeAliasesProperty -> storeAliasesProperty.split("\\s*,\\s*"))
              .orElse(null);
      final KeyStoreCredentialConfig keyStoreCredentialConfig = new KeyStoreCredentialConfig();
      keyStoreCredentialConfig.setTrustStore(trustStore);
      keyStoreCredentialConfig.setTrustStorePassword(trustStorePassword);
      keyStoreCredentialConfig.setTrustStoreType(trustStoreType);
      keyStoreCredentialConfig.setTrustStoreAliases(trustStoreAliases);
      credentialConfig = keyStoreCredentialConfig;
    } else if (StringUtils.notBlank(keyStore)) {
      final String keyStorePassword =
          properties.get(AvailableAppSettings.AUTH_LDAP_SERVER_SSL_KEY_STORE_PASSWORD);
      final String keyStoreType =
          properties.get(AvailableAppSettings.AUTH_LDAP_SERVER_SSL_KEY_STORE_TYPE);
      final String[] keyStoreAliases =
          Optional.ofNullable(
                  properties.get(AvailableAppSettings.AUTH_LDAP_SERVER_SSL_KEY_STORE_ALIASES))
              .map(storeAliasesProperty -> storeAliasesProperty.split("\\s*,\\s*"))
              .orElse(null);
      final KeyStoreCredentialConfig keyStoreCredentialConfig = new KeyStoreCredentialConfig();
      keyStoreCredentialConfig.setKeyStore(keyStore);
      keyStoreCredentialConfig.setKeyStorePassword(keyStorePassword);
      keyStoreCredentialConfig.setKeyStoreType(keyStoreType);
      keyStoreCredentialConfig.setKeyStoreAliases(keyStoreAliases);
      credentialConfig = keyStoreCredentialConfig;
    } else if (StringUtils.notBlank(trustCertificates)) {
      final String authenticationCertificate =
          properties.get(AvailableAppSettings.AUTH_LDAP_SERVER_SSL_CERT_AUTH_PATH);
      final String authenticationKey =
          properties.get(AvailableAppSettings.AUTH_LDAP_SERVER_SSL_CERT_KEY_PATH);
      final X509CredentialConfig x509CredentialConfig = new X509CredentialConfig();
      x509CredentialConfig.setTrustCertificates(trustCertificates);
      x509CredentialConfig.setAuthenticationCertificate(authenticationCertificate);
      x509CredentialConfig.setAuthenticationKey(authenticationKey);
      credentialConfig = x509CredentialConfig;
    } else {
      credentialConfig = null;
    }

    groupsDn = properties.get(AvailableAppSettings.AUTH_LDAP_GROUP_BASE);
    groupFilter = properties.get(AvailableAppSettings.AUTH_LDAP_GROUP_FILTER);

    if (credentialConfig != null) {
      sslConfig = new SslConfig(credentialConfig);
    } else {
      sslConfig = null;
    }

    final ConnectionConfig config = new ConnectionConfig(ldapUrl);
    if (useStartTLS) {
      config.setUseStartTLS(useStartTLS);
    }
    if (sslConfig != null) {
      config.setSslConfig(sslConfig);
    }
    if (connectTimeout != null) {
      config.setConnectTimeout(connectTimeout);
    }
    if (responseTimeout != null) {
      config.setResponseTimeout(responseTimeout);
    }

    final BindConnectionInitializer initializer;
    if (StringUtils.notBlank(systemDn)) {
      initializer = new BindConnectionInitializer(systemDn, new Credential(systemPassword));
      if (saslConfig != null) {
        initializer.setBindSaslConfig(saslConfig);
      }
      config.setConnectionInitializers(initializer);
    } else {
      initializer = null;
    }

    final PooledConnectionFactory factory = new PooledConnectionFactory(config);
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  if (factory.isInitialized()) {
                    factory.close();
                  }
                }));
    if (initializer != null) {
      factory.setActivator(
          conn -> {
            try {
              return initializer.initialize(conn).isSuccess();
            } catch (LdapException e) {
              logger.error(e.getMessage(), e);
              return false;
            }
          });
    }

    final SimpleBindAuthenticationHandler handler = new SimpleBindAuthenticationHandler(factory);

    final DnResolver dnResolver;
    if (StringUtils.notBlank(userFilter)) {
      final SearchDnResolver searchDnResolver = new SearchDnResolver(factory);
      searchDnResolver.setBaseDn(usersDn);
      searchDnResolver.setUserFilter(userFilter);
      dnResolver = searchDnResolver;
    } else {
      final String format =
          StringUtils.notBlank(userDnFormat)
              ? userDnFormat
              : String.format("%s=%%s,%s", idAttribute, usersDn);
      dnResolver = new FormatDnResolver(format);
    }

    final Authenticator authenticator = new Authenticator(dnResolver, handler);
    authenticator.setEntryResolver(new AxelorSearchEntryResolver());

    final String attributes = AxelorLdapProfileDefinition.getAttributes(idAttribute);

    setConnectionFactory(factory);
    setLdapAuthenticator(authenticator);
    setAttributes(attributes);
    setUsersDn(usersDn);

    setIdAttribute(idAttribute);
    setUsernameAttribute(usernameAttribute);
    setPasswordAttribute(AxelorLdapProfileDefinition.PASSWORD);
    setProfileDefinition(new AxelorLdapProfileDefinition());
  }

  @Override
  protected boolean shouldInitialize(boolean forceReinit) {
    final ConnectionFactory factory = getConnectionFactory();
    return super.shouldInitialize(forceReinit)
        || factory instanceof PooledConnectionFactory
            && !((PooledConnectionFactory) factory).isInitialized();
  }

  @Override
  protected void internalInit(boolean forceReinit) {
    final ConnectionFactory factory = getConnectionFactory();
    if (factory instanceof PooledConnectionFactory) {
      ((PooledConnectionFactory) factory).initialize();
    }
    super.internalInit(forceReinit);
  }

  @Override
  public void validate(Credentials credentials, WebContext context, SessionStore sessionStore) {
    if (Optional.ofNullable(credentials)
        .filter(UsernamePasswordCredentials.class::isInstance)
        .map(UsernamePasswordCredentials.class::cast)
        .map(UsernamePasswordCredentials::getUsername)
        .filter(StringUtils::notBlank)
        .isEmpty()) {
      throw new BadCredentialsException("Username cannot be blank.");
    }
    super.validate(credentials, context, sessionStore);
  }

  public String getGroupsDn() {
    return groupsDn;
  }

  public String getGroupFilter() {
    return groupFilter;
  }

  @Nullable
  public LdapEntry searchGroup(String groupName) {
    final String filter = String.format(FILTER_FORMAT, AxelorLdapGroupDefinition.NAME, groupName);
    final SearchRequest request =
        new SearchRequest(
            groupsDn, filter, AxelorLdapGroupDefinition.ATTRIBUTES.stream().toArray(String[]::new));
    final SearchOperation search = new SearchOperation(getConnectionFactory());
    final SearchResponse response;

    try {
      response = search.execute(request);
    } catch (LdapException e) {
      logger.error(e.getMessage(), e);
      return null;
    }

    return response.getEntry();
  }

  @Nullable
  protected SaslConfig getSaslConfig(String authenticationType) {
    final String mechanismName =
        StringUtils.isBlank(authenticationType)
            ? "SIMPLE"
            : authenticationType.toUpperCase().replace('-', '_');

    switch (mechanismName) {
      case "NONE":
      case "SIMPLE":
        return null;
      default:
        final SaslConfig saslConfig = new SaslConfig();
        saslConfig.setMechanism(Mechanism.valueOf(mechanismName));
        return saslConfig;
    }
  }

  @Override
  protected LdapProfile convertAttributesToProfile(
      List<Map<String, Object>> listStorageAttributes, String username) {
    final LdapProfile profile = super.convertAttributesToProfile(listStorageAttributes, username);
    if (ObjectUtils.isEmpty(profile.getAttributes())) {
      searchAndSetAttributes(profile);
    }
    setGroup(profile);
    setRoles(profile);
    return profile;
  }

  protected void searchAndSetAttributes(LdapProfile profile) {
    final SearchOperation search = new SearchOperation(getConnectionFactory());
    search.setEntryHandlers(new ObjectSidHandler(), new ObjectGuidHandler());
    final SearchRequest request =
        new SearchRequest(
            getUsersDn(), String.format(FILTER_FORMAT, getIdAttribute(), profile.getId()));
    final SearchResponse response;

    try {
      response = search.execute(request);
    } catch (LdapException e) {
      logger.error(e.getMessage(), e);
      return;
    }

    final Set<String> attributeKeys = profile.getAttributes().keySet();
    final LdapEntry entry = response.getEntry();

    if (entry == null) {
      logger.error("No entry found with search filter: {}", request.getFilter());
      return;
    }

    entry.getAttributes().stream()
        .filter(attribute -> !attributeKeys.contains(attribute.getName()))
        .forEach(
            attribute -> {
              final Object value =
                  attribute.size() > 1 ? attribute.getStringValues() : attribute.getStringValue();
              profile.addAttribute(attribute.getName(), value);
            });
  }

  protected void setGroup(LdapProfile profile) {
    if (StringUtils.isBlank(groupsDn)) {
      return;
    }

    try {
      // Search through configured filter
      if (StringUtils.notBlank(groupFilter)) {
        setGroup(profile, MessageFormat.format(groupFilter, profile.getId()));
        return;
      }

      // Search posixGroup
      final Integer groupId = (Integer) profile.getAttribute(AxelorLdapGroupDefinition.ID);
      if (groupId == null
          || setGroup(profile, String.format("(%s=%d)", AxelorLdapGroupDefinition.ID, groupId))
              == null) {
        final String entryId = getEntryId(convertProfileAndPasswordToAttributes(profile, null));

        // Search groupOfUniqueNames, groupOfNames, and group
        for (final String memberAttribute :
            List.of(AxelorLdapGroupDefinition.UNIQUE_MEMBER, AxelorLdapGroupDefinition.MEMBER)) {
          if (setGroup(profile, String.format(FILTER_FORMAT, memberAttribute, entryId)) != null) {
            break;
          }
        }
      }

    } catch (LdapException e) {
      logger.error(e.getMessage(), e);
    }
  }

  @Nullable
  protected String setGroup(LdapProfile profile, String filter) throws LdapException {
    final SearchRequest request =
        new SearchRequest(groupsDn, filter, AxelorLdapGroupDefinition.NAME);
    final SearchOperation search = new SearchOperation(getConnectionFactory());
    final SearchResponse response = search.execute(request);
    final LdapEntry entry = response.getEntry();

    if (entry != null) {
      final String groupName = entry.getAttribute(AxelorLdapGroupDefinition.NAME).getStringValue();
      profile.addAttribute(AuthPac4jProfileService.GROUP_ATTRIBUTE, groupName);
      return groupName;
    }

    return null;
  }

  // Set groups found via memberOf as roles
  protected void setRoles(LdapProfile profile) {
    final Object attr = profile.getAttribute("memberOf");

    if (ObjectUtils.isEmpty(attr)) {
      return;
    }

    @SuppressWarnings("unchecked")
    final Collection<String> memberOf =
        attr instanceof Collection
            ? (Collection<String>) attr
            : Collections.singletonList((String) attr);

    memberOf.forEach(
        groupDn ->
            Pattern.compile("\\s*,\\s*")
                .splitAsStream(groupDn)
                .map(item -> item.split("\\s*=\\s*", 2))
                .findFirst()
                .ifPresent(item -> profile.addRole(item[1])));
  }

  // Fix binary attributes
  @Override
  protected List<LdapAttribute> getLdapAttributes(Map<String, Object> attributes) {
    final List<LdapAttribute> ldapAttributes = super.getLdapAttributes(attributes);
    final List<LdapAttribute> binaryLdapAttributes = new ArrayList<>();

    for (final Iterator<LdapAttribute> it = ldapAttributes.iterator(); it.hasNext(); ) {
      final LdapAttribute ldapAttribute = it.next();
      final String name = ldapAttribute.getName();
      final Object value = attributes.get(name);
      if (value instanceof byte[]) {
        it.remove();
        binaryLdapAttributes.add(new LdapAttribute(name, (byte[]) value));
      }
    }

    ldapAttributes.addAll(binaryLdapAttributes);
    return ldapAttributes;
  }
}
