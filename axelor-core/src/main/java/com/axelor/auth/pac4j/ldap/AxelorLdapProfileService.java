/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
import com.google.common.collect.ImmutableList;
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
import org.ldaptive.Connection;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.Credential;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.SearchOperation;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchResult;
import org.ldaptive.ad.handler.ObjectGuidHandler;
import org.ldaptive.ad.handler.ObjectSidHandler;
import org.ldaptive.auth.Authenticator;
import org.ldaptive.auth.DnResolver;
import org.ldaptive.auth.FormatDnResolver;
import org.ldaptive.auth.PooledBindAuthenticationHandler;
import org.ldaptive.auth.SearchDnResolver;
import org.ldaptive.pool.BlockingConnectionPool;
import org.ldaptive.pool.PooledConnectionFactory;
import org.ldaptive.sasl.CramMd5Config;
import org.ldaptive.sasl.DigestMd5Config;
import org.ldaptive.sasl.ExternalConfig;
import org.ldaptive.sasl.GssApiConfig;
import org.ldaptive.sasl.Mechanism;
import org.ldaptive.sasl.SaslConfig;
import org.ldaptive.ssl.CredentialConfig;
import org.ldaptive.ssl.KeyStoreCredentialConfig;
import org.ldaptive.ssl.SslConfig;
import org.ldaptive.ssl.X509CredentialConfig;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.exception.BadCredentialsException;
import org.pac4j.ldap.profile.LdapProfile;
import org.pac4j.ldap.profile.service.LdapProfileService;

@Singleton
public class AxelorLdapProfileService extends LdapProfileService {

  private final String groupsDn;
  private final String groupFilter;

  protected static final String FILTER_FORMAT = "(%s=%s)";

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
    final String systemDn = properties.get(AvailableAppSettings.AUTH_LDAP_SYSTEM_USER);
    final String systemPassword = properties.get(AvailableAppSettings.AUTH_LDAP_SYSTEM_PASSWORD);
    final String authenticationType = properties.get(AvailableAppSettings.AUTH_LDAP_AUTH_TYPE);
    final boolean useSSL =
        Optional.ofNullable(properties.getOrDefault(AvailableAppSettings.AUTH_LDAP_USE_SSL, null))
            .map(Boolean::parseBoolean)
            .orElseGet(() -> ldapUrl != null && ldapUrl.toLowerCase().startsWith("ldaps:"));
    final boolean useStartTLS =
        Boolean.parseBoolean(properties.get(AvailableAppSettings.AUTH_LDAP_USE_STARTTLS));
    final String trustStore = properties.get(AvailableAppSettings.AUTH_LDAP_CREDENTIAL_TRUST_STORE);
    final String keyStore = properties.get(AvailableAppSettings.AUTH_LDAP_CREDENTIAL_KEY_STORE);
    final String trustCertificates =
        properties.get(AvailableAppSettings.AUTH_LDAP_CREDENTIAL_TRUST_CERTIFICATES);
    final Duration connectTimeout =
        Optional.ofNullable(properties.get(AvailableAppSettings.AUTH_LDAP_CONNECT_TIMEOUT))
            .filter(StringUtils::notBlank)
            .map(Long::parseLong)
            .map(Duration::ofSeconds)
            .orElse(null);
    final Duration responseTimeout =
        Optional.ofNullable(properties.get(AvailableAppSettings.AUTH_LDAP_RESPONSE_TIMEOUT))
            .filter(StringUtils::notBlank)
            .map(Long::parseLong)
            .map(Duration::ofSeconds)
            .orElse(null);

    final SaslConfig saslConfig = getSaslConfig(authenticationType);
    final SslConfig sslConfig;
    final CredentialConfig credentialConfig;

    if (StringUtils.notBlank(trustStore) || StringUtils.notBlank(keyStore)) {
      final String storePassword =
          properties.get(AvailableAppSettings.AUTH_LDAP_CREDENTIAL_STORE_PASSWORD);
      final String storeType = properties.get(AvailableAppSettings.AUTH_LDAP_CREDENTIAL_STORE_TYPE);
      final String[] storeAliases =
          Optional.ofNullable(
                  properties.get(AvailableAppSettings.AUTH_LDAP_CREDENTIAL_STORE_ALIASES))
              .map(storeAliasesProperty -> storeAliasesProperty.split("\\s*,\\s*"))
              .orElse(null);
      final KeyStoreCredentialConfig keyStoreCredentialConfig = new KeyStoreCredentialConfig();
      if (StringUtils.notBlank(trustStore)) {
        keyStoreCredentialConfig.setTrustStore(trustStore);
        keyStoreCredentialConfig.setTrustStorePassword(storePassword);
        keyStoreCredentialConfig.setTrustStoreType(storeType);
        keyStoreCredentialConfig.setTrustStoreAliases(storeAliases);
      } else {
        keyStoreCredentialConfig.setKeyStore(keyStore);
        keyStoreCredentialConfig.setKeyStorePassword(storePassword);
        keyStoreCredentialConfig.setKeyStoreType(storeType);
        keyStoreCredentialConfig.setKeyStoreAliases(storeAliases);
      }
      credentialConfig = keyStoreCredentialConfig;
    } else if (StringUtils.notBlank(trustCertificates)) {
      final String authenticationCertificate =
          properties.get(AvailableAppSettings.AUTH_LDAP_CREDENTIAL_AUTHENTICATION_CERTIFICATE);
      final String authenticationKey =
          properties.get(AvailableAppSettings.AUTH_LDAP_CREDENTIAL_AUTHENTICATION_KEY);
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

    final ConnectionConfig connectionConfig = new ConnectionConfig(ldapUrl);
    if (useSSL) {
      connectionConfig.setUseSSL(useSSL);
    }
    if (useStartTLS) {
      connectionConfig.setUseStartTLS(useStartTLS);
    }
    if (StringUtils.notBlank(systemDn)) {
      final BindConnectionInitializer initializer =
          new BindConnectionInitializer(systemDn, new Credential(systemPassword));
      if (saslConfig != null) {
        initializer.setBindSaslConfig(saslConfig);
      }
      connectionConfig.setConnectionInitializer(initializer);
    }
    if (sslConfig != null) {
      connectionConfig.setSslConfig(sslConfig);
    }
    if (connectTimeout != null) {
      connectionConfig.setConnectTimeout(connectTimeout);
    }
    if (responseTimeout != null) {
      connectionConfig.setResponseTimeout(responseTimeout);
    }

    final DefaultConnectionFactory connectionFactory =
        new DefaultConnectionFactory(connectionConfig);

    final BlockingConnectionPool connectionPool = new BlockingConnectionPool(connectionFactory);
    connectionPool.initialize();
    final PooledConnectionFactory pooledConnectionFactory =
        new PooledConnectionFactory(connectionPool);
    final PooledBindAuthenticationHandler handler =
        new PooledBindAuthenticationHandler(pooledConnectionFactory);

    final DnResolver dnResolver;
    if (StringUtils.notBlank(userFilter)) {
      final SearchDnResolver searchDnResolver = new SearchDnResolver(connectionFactory);
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

    final Authenticator ldapAuthenticator = new Authenticator(dnResolver, handler);
    ldapAuthenticator.setEntryResolver(new AxelorSearchEntryResolver());

    final String attributes = AxelorLdapProfileDefinition.getAttributes(idAttribute);

    setConnectionFactory(connectionFactory);
    setLdapAuthenticator(ldapAuthenticator);
    setAttributes(attributes);
    setUsersDn(usersDn);

    setIdAttribute(idAttribute);
    setUsernameAttribute(usernameAttribute);
    setPasswordAttribute(AxelorLdapProfileDefinition.PASSWORD);
    setProfileDefinition(new AxelorLdapProfileDefinition());
  }

  @Override
  public void validate(UsernamePasswordCredentials credentials, WebContext context) {
    if (credentials == null || StringUtils.isBlank(credentials.getUsername())) {
      throw new BadCredentialsException("Username cannot be blank.");
    }
    super.validate(credentials, context);
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

    try (final Connection conn = getConnectionFactory().getConnection()) {
      conn.open();

      final SearchRequest request =
          new SearchRequest(
              groupsDn,
              filter,
              AxelorLdapGroupDefinition.ATTRIBUTES.stream().toArray(String[]::new));
      final SearchOperation search = new SearchOperation(conn);
      final SearchResult result = search.execute(request).getResult();
      return result.getEntry();

    } catch (LdapException e) {
      logger.error(e.getMessage(), e);
    }

    return null;
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
      case "CRAM_MD5":
        return new CramMd5Config();
      case "DIGEST_MD5":
        return new DigestMd5Config();
      case "EXTERNAL":
        return new ExternalConfig();
      case "GSSAPI":
        return new GssApiConfig();
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
    try (final Connection conn = getConnectionFactory().getConnection()) {
      conn.open();

      final SearchOperation search = new SearchOperation(conn);
      final SearchRequest request =
          new SearchRequest(
              getUsersDn(), String.format(FILTER_FORMAT, getIdAttribute(), profile.getId()));

      request.setSearchEntryHandlers(new ObjectSidHandler(), new ObjectGuidHandler());

      final SearchResult result = search.execute(request).getResult();
      final Set<String> attributeKeys = profile.getAttributes().keySet();
      final LdapEntry entry = result.getEntry();

      if (entry == null) {
        logger.error(
            "No entry found with search filter: {}", request.getSearchFilter().getFilter());
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

    } catch (LdapException e) {
      logger.error(e.getMessage(), e);
    }
  }

  protected void setGroup(LdapProfile profile) {
    if (StringUtils.isBlank(groupsDn)) {
      return;
    }

    try (final Connection conn = getConnectionFactory().getConnection()) {
      conn.open();

      // Search through configured filter
      if (StringUtils.notBlank(groupFilter)) {
        setGroup(profile, MessageFormat.format(groupFilter, profile.getId()), conn);
        return;
      }

      // Search posixGroup
      final Integer groupId = (Integer) profile.getAttribute(AxelorLdapGroupDefinition.ID);
      if (groupId == null
          || setGroup(
                  profile, String.format("(%s=%d)", AxelorLdapGroupDefinition.ID, groupId), conn)
              == null) {
        final String entryId = getEntryId(convertProfileAndPasswordToAttributes(profile, null));

        // Search groupOfUniqueNames, groupOfNames, and group
        for (final String memberAttribute :
            ImmutableList.of(
                AxelorLdapGroupDefinition.UNIQUE_MEMBER, AxelorLdapGroupDefinition.MEMBER)) {
          if (setGroup(profile, String.format(FILTER_FORMAT, memberAttribute, entryId), conn)
              != null) {
            break;
          }
        }
      }

    } catch (LdapException e) {
      logger.error(e.getMessage(), e);
    }
  }

  @Nullable
  protected String setGroup(LdapProfile profile, String filter, Connection conn)
      throws LdapException {
    final SearchRequest request =
        new SearchRequest(groupsDn, filter, AxelorLdapGroupDefinition.NAME);
    final SearchOperation search = new SearchOperation(conn);
    final SearchResult result = search.execute(request).getResult();
    final LdapEntry entry = result.getEntry();

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
