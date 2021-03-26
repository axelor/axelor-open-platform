/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2021 Axelor (<http://axelor.com>).
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
import com.axelor.auth.pac4j.AuthPac4jModuleLocal.AxelorAuthenticator;
import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import org.pac4j.core.profile.converter.AbstractAttributeConverter;
import org.pac4j.core.profile.converter.Converters;
import org.pac4j.core.profile.definition.CommonProfileDefinition;
import org.pac4j.ldap.profile.LdapProfile;
import org.pac4j.ldap.profile.service.LdapProfileService;

@Singleton
public class AxelorLdapProfileService extends LdapProfileService implements AxelorAuthenticator {

  private final String groupsDn;
  private final String groupFilter;

  @Inject
  public AxelorLdapProfileService() {
    this(AppSettings.get().getProperties());
  }

  public AxelorLdapProfileService(Properties properties) {
    final String ldapUrl = properties.getProperty(AvailableAppSettings.AUTH_LDAP_SERVER_URL);
    final String usersDn = properties.getProperty(AvailableAppSettings.AUTH_LDAP_USER_BASE);
    final String idAttribute =
        properties.getProperty(
            AvailableAppSettings.AUTH_LDAP_USER_ID_ATTRIBUTE, AxelorLdapProfileDefinition.USERNAME);
    final String usernameAttribute =
        properties.getProperty(
            AvailableAppSettings.AUTH_LDAP_USER_USERNAME_ATTRIBUTE,
            AxelorLdapProfileDefinition.USERNAME);
    final String userFilter =
        Optional.ofNullable(properties.getProperty(AvailableAppSettings.AUTH_LDAP_USER_FILTER))
            .map(property -> property.replace("{0}", "{user}"))
            .orElse(null);
    final String userDnFormat =
        properties.getProperty(AvailableAppSettings.AUTH_LDAP_USER_DN_FORMAT, null);
    final String systemDn = properties.getProperty(AvailableAppSettings.AUTH_LDAP_SYSTEM_USER);
    final String systemPassword =
        properties.getProperty(AvailableAppSettings.AUTH_LDAP_SYSTEM_PASSWORD);
    final String authenticationType =
        properties.getProperty(AvailableAppSettings.AUTH_LDAP_AUTH_TYPE);
    final boolean useSSL =
        Optional.ofNullable(properties.getProperty(AvailableAppSettings.AUTH_LDAP_USE_SSL, null))
            .map(Boolean::parseBoolean)
            .orElseGet(() -> ldapUrl != null && ldapUrl.toLowerCase().startsWith("ldaps:"));
    final boolean useStartTLS =
        Boolean.parseBoolean(properties.getProperty(AvailableAppSettings.AUTH_LDAP_USE_STARTTLS));
    final String trustStore =
        properties.getProperty(AvailableAppSettings.AUTH_LDAP_CREDENTIAL_TRUST_STORE);
    final String keyStore =
        properties.getProperty(AvailableAppSettings.AUTH_LDAP_CREDENTIAL_KEY_STORE);
    final String trustCertificates =
        properties.getProperty(AvailableAppSettings.AUTH_LDAP_CREDENTIAL_TRUST_CERTIFICATES);
    final Duration connectTimeout =
        Optional.ofNullable(properties.getProperty(AvailableAppSettings.AUTH_LDAP_CONNECT_TIMEOUT))
            .filter(StringUtils::notBlank)
            .map(Long::parseLong)
            .map(Duration::ofSeconds)
            .orElse(null);
    final Duration responseTimeout =
        Optional.ofNullable(properties.getProperty(AvailableAppSettings.AUTH_LDAP_RESPONSE_TIMEOUT))
            .filter(StringUtils::notBlank)
            .map(Long::parseLong)
            .map(Duration::ofSeconds)
            .orElse(null);

    final SaslConfig saslConfig = getSaslConfig(authenticationType);
    final SslConfig sslConfig;
    final CredentialConfig credentialConfig;

    if (StringUtils.notBlank(trustStore) || StringUtils.notBlank(keyStore)) {
      final String storePassword =
          properties.getProperty(AvailableAppSettings.AUTH_LDAP_CREDENTIAL_STORE_PASSWORD);
      final String storeType =
          properties.getProperty(AvailableAppSettings.AUTH_LDAP_CREDENTIAL_STORE_TYPE);
      final String[] storeAliases =
          Optional.ofNullable(
                  properties.getProperty(AvailableAppSettings.AUTH_LDAP_CREDENTIAL_STORE_ALIASES))
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
          properties.getProperty(
              AvailableAppSettings.AUTH_LDAP_CREDENTIAL_AUTHENTICATION_CERTIFICATE);
      final String authenticationKey =
          properties.getProperty(AvailableAppSettings.AUTH_LDAP_CREDENTIAL_AUTHENTICATION_KEY);
      final X509CredentialConfig x509CredentialConfig = new X509CredentialConfig();
      x509CredentialConfig.setTrustCertificates(trustCertificates);
      x509CredentialConfig.setAuthenticationCertificate(authenticationCertificate);
      x509CredentialConfig.setAuthenticationKey(authenticationKey);
      credentialConfig = x509CredentialConfig;
    } else {
      credentialConfig = null;
    }

    groupsDn = properties.getProperty(AvailableAppSettings.AUTH_LDAP_GROUP_BASE);
    groupFilter = properties.getProperty(AvailableAppSettings.AUTH_LDAP_GROUP_FILTER);

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
      SearchDnResolver searchDnResolver = new SearchDnResolver(connectionFactory);
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

  @Nullable
  public LdapEntry searchGroup(String groupName) {
    final String filter = String.format("(%s=%s)", AxelorLdapGroupDefinition.NAME, groupName);

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
    setGroup(profile);
    return profile;
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
          if (setGroup(profile, String.format("(%s=%s)", memberAttribute, entryId), conn) != null) {
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

  public static class AxelorLdapGroupDefinition {
    public static final String NAME = "cn";
    public static final String ID = "gidNumber";
    public static final String OBJECT_CLASS = "objectClass";
    public static final String UNIQUE_MEMBER = "uniqueMember";
    public static final String MEMBER = "member";

    public static final List<String> ATTRIBUTES =
        ImmutableList.of(NAME, ID, OBJECT_CLASS, UNIQUE_MEMBER, MEMBER);
  }

  public static class AxelorLdapProfileDefinition extends CommonProfileDefinition<LdapProfile> {

    public static final String USERNAME = "uid";
    public static final String DISPLAY_NAME = "cn";
    public static final String EMAIL = "mail";
    public static final String FIRST_NAME = "givenName";
    public static final String FAMILY_NAME = "sn";
    public static final String LOCALE = "preferredLanguage";
    public static final String PICTURE_JPEG = "jpegPhoto";
    public static final String PASSWORD = "userPassword";

    public static final List<String> ATTRIBUTES =
        ImmutableList.of(
            USERNAME,
            EMAIL,
            DISPLAY_NAME,
            FIRST_NAME,
            FAMILY_NAME,
            LOCALE,
            PICTURE_JPEG,
            AxelorLdapGroupDefinition.ID);

    public AxelorLdapProfileDefinition() {
      super(x -> new AxelorLdapProfile());
      Stream.of(USERNAME, EMAIL, DISPLAY_NAME, FIRST_NAME, FAMILY_NAME)
          .forEach(attribute -> primary(attribute, Converters.STRING));
      primary(LOCALE, Converters.LOCALE);
      primary(AxelorLdapGroupDefinition.ID, Converters.INTEGER);
      primary(PICTURE_JPEG, ByteArrayConverter.INSTANCE);
    }

    public static String getAttributes(String idAttribute) {
      final Set<String> excludedAttributes = Sets.newHashSet(USERNAME);
      switch (idAttribute) {
        case USERNAME:
          break;
        case DISPLAY_NAME:
          excludedAttributes.add(DISPLAY_NAME);
          break;
        default:
          throw new IllegalArgumentException(
              String.format("Illegal ID attribute: %s", idAttribute));
      }
      return ATTRIBUTES.stream()
          .filter(attribute -> !excludedAttributes.contains(attribute))
          .collect(Collectors.joining(","));
    }
  }

  public static class AxelorLdapProfile extends LdapProfile {
    private transient Path picturePath;

    @Override
    public String getUsername() {
      return getId();
    }

    @Override
    public String getEmail() {
      return (String) getAttribute(AxelorLdapProfileDefinition.EMAIL);
    }

    @Override
    public String getDisplayName() {
      return (String) getAttribute(AxelorLdapProfileDefinition.DISPLAY_NAME);
    }

    @Override
    public String getFirstName() {
      return (String) getAttribute(AxelorLdapProfileDefinition.FIRST_NAME);
    }

    @Override
    public String getFamilyName() {
      return (String) getAttribute(AxelorLdapProfileDefinition.FAMILY_NAME);
    }

    @Override
    public Locale getLocale() {
      return (Locale) getAttribute(AxelorLdapProfileDefinition.LOCALE);
    }

    @Override
    public URI getPictureUrl() {
      if (picturePath != null && picturePath.toFile().exists()) {
        return picturePath.toUri();
      }

      final byte[] jpegPhoto = (byte[]) getAttribute(AxelorLdapProfileDefinition.PICTURE_JPEG);
      if (ObjectUtils.notEmpty(jpegPhoto)) {
        try {
          picturePath = Files.createTempFile(null, ".jpg");
          Files.write(picturePath, jpegPhoto);
          return picturePath.toUri();
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }

      return null;
    }
  }

  public static class ByteArrayConverter extends AbstractAttributeConverter<byte[]> {

    public static final ByteArrayConverter INSTANCE = new ByteArrayConverter();

    protected ByteArrayConverter() {
      super(byte[].class);
    }

    @Override
    protected byte[] internalConvert(Object attribute) {
      return ObjectUtils.isEmpty(attribute)
          ? null
          : Base64.getDecoder().decode(attribute.toString());
    }
  }
}
