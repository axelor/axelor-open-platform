/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.auth.pac4j;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.Inflector;
import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.inject.Beans;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Provider;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.jasig.cas.client.util.PrivateKeyUtils;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import org.pac4j.http.client.indirect.IndirectBasicAuthClient;
import org.pac4j.ldap.profile.service.LdapProfileService;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.config.PrivateKeyJWTClientAuthnMethodConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ClientListProvider implements Provider<List<Client>> {

  private final List<Client> clients = new ArrayList<>();

  private final String defaultClientName;

  private final Set<String> indirectClientNames;

  private final Set<String> directClientNames;

  private final boolean exclusive;

  // Default client configurations
  private static final Map<String, ClientConfig> CONFIGS =
      new ImmutableMap.Builder<String, ClientConfig>()
          .put(
              "form",
              ClientConfig.builder().client("org.pac4j.http.client.indirect.FormClient").build())
          .put(
              "oidc",
              ClientConfig.builder()
                  .client("org.pac4j.oidc.client.OidcClient")
                  .title("OpenID Connect")
                  .icon("img/signin/openid.svg")
                  .build())
          .put(
              "keycloak",
              ClientConfig.builder()
                  .client("org.pac4j.oidc.client.KeycloakOidcClient")
                  .title("Keycloak")
                  .icon("img/signin/keycloak.svg")
                  .build())
          .put(
              "google",
              ClientConfig.builder()
                  .client("org.pac4j.oidc.client.GoogleOidcClient")
                  .title("Google")
                  .icon("img/signin/google.svg")
                  .build())
          .put(
              "azure",
              ClientConfig.builder()
                  .client("org.pac4j.oidc.client.AzureAd2Client")
                  .title("Azure Active Directory")
                  .icon("img/signin/microsoft.svg")
                  .build())
          .put(
              "apple",
              ClientConfig.builder()
                  .client("org.pac4j.oidc.client.AppleClient")
                  .title("Apple")
                  .icon("img/signin/apple.svg")
                  .build())
          .put(
              "oauth",
              ClientConfig.builder()
                  .client("org.pac4j.oauth.client.GenericOAuth20Client")
                  .title("OAuth 2.0")
                  .icon("img/signin/oauth.svg")
                  .build())
          .put(
              "facebook",
              ClientConfig.builder()
                  .client("org.pac4j.oauth.client.FacebookClient")
                  .title("Facebook")
                  .icon("img/signin/facebook.svg")
                  .build())
          .put(
              "github",
              ClientConfig.builder()
                  .client("org.pac4j.oauth.client.GitHubClient")
                  .title("GitHub")
                  .icon("img/signin/github.svg")
                  .build())
          .put(
              "saml",
              ClientConfig.builder()
                  .client("org.pac4j.saml.client.SAML2Client")
                  .title("SAML 2.0")
                  .icon("img/signin/saml.svg")
                  .exclusive()
                  .build())
          .put(
              "cas",
              ClientConfig.builder()
                  .client("org.pac4j.cas.client.CasClient")
                  .title("CAS")
                  .icon("img/signin/cas.png")
                  .exclusive()
                  .build())
          .build();

  private static final Map<String, BiFunction<AppSettings, String, Object>> SETTINGS_GETTERS =
      Map.of("exclusive", (settings, key) -> settings.getBoolean(key, false));

  private static final Set<String> EXTRA_CONFIGS =
      Set.of("client", "configuration", "title", "icon", "exclusive");

  private static final Map<Class<?>, Class<?>> PRIMITIVE_TYPES =
      new ImmutableMap.Builder<Class<?>, Class<?>>()
          .put(byte.class, Byte.class)
          .put(short.class, Short.class)
          .put(int.class, Integer.class)
          .put(long.class, Long.class)
          .put(float.class, Float.class)
          .put(double.class, Double.class)
          .put(boolean.class, Boolean.class)
          .put(char.class, Character.class)
          .build();

  private static final Map<Class<?>, UnaryOperator<Object>> CONVERTERS =
      new ImmutableMap.Builder<Class<?>, UnaryOperator<Object>>()
          .put(
              PrivateKeyJWTClientAuthnMethodConfig.class,
              value -> {
                @SuppressWarnings("unchecked")
                final Map<String, Object> map = getCamelizedMapKeys((Map<String, Object>) value);
                final String privateKeyPath = (String) map.get("privateKey.path");
                final String privateKeyAlgorithm =
                    (String) map.getOrDefault("privateKey.algorithm", "RSA");
                final JWSAlgorithm jwsAlgorithm =
                    JWSAlgorithm.parse((String) map.getOrDefault("jwsAlgorithm", "RS256"));
                final String keyId = (String) map.get("keyId");
                final PrivateKey privateKey =
                    PrivateKeyUtils.createKey(privateKeyPath, privateKeyAlgorithm);
                return new PrivateKeyJWTClientAuthnMethodConfig(jwsAlgorithm, privateKey, keyId);
              })
          .build();

  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final Pattern AUTH_PROVIDER_PATTERN =
      Pattern.compile("auth\\.provider\\.(?<name>[^.]+)\\.(?<config>.*)");

  @Inject
  public ClientListProvider(AuthPac4jInfo authPac4jInfo) {
    final Map<String, Map<String, Object>> initConfigs = new LinkedHashMap<>();
    final AppSettings settings = AppSettings.get();
    final Map<String, String> properties = settings.getInternalProperties();
    final String ldapServerUrl = settings.get(AvailableAppSettings.AUTH_LDAP_SERVER_URL, null);

    // LDAP
    if (ldapServerUrl != null) {
      authPac4jInfo.setAuthenticator(Beans.get(LdapProfileService.class));
      logger.info("LDAP: {}", ldapServerUrl);
    }

    // prepare client config
    for (String key : properties.keySet()) {
      Matcher matcher = AUTH_PROVIDER_PATTERN.matcher(key);
      if (matcher.matches()) {
        String name = matcher.group("name");
        String[] config = matcher.group("config").split("\\.", 2);
        config[0] = Inflector.getInstance().camelize(config[0], true);
        Object value =
            SETTINGS_GETTERS.getOrDefault(config[0], (s, k) -> s.get(k, null)).apply(settings, key);

        final Map<String, Object> initConfig =
            initConfigs.computeIfAbsent(name, k -> new HashMap<>());

        if (config.length > 1) {
          // string-object map config
          @SuppressWarnings("unchecked")
          final Map<String, Object> map =
              (Map<String, Object>) initConfig.computeIfAbsent(config[0], k -> new HashMap<>());
          map.put(config[1], value);
        } else {
          initConfig.put(config[0], value);
        }
      }
    }

    // set default values
    initConfigs
        .entrySet()
        .forEach(
            entry -> {
              final String name = entry.getKey();
              final ClientConfig config = CONFIGS.get(name);
              if (config == null) {
                return;
              }
              final Map<String, Object> props = entry.getValue();
              config.toMap().forEach(props::putIfAbsent);
            });

    // add form as if no exclusive client
    if (initConfigs.isEmpty()
        || initConfigs.size() > 1
        || !((boolean) initConfigs.values().iterator().next().getOrDefault("exclusive", false))) {
      initConfigs.put("form", CONFIGS.get("form").toMap());
      exclusive = false;
    } else {
      exclusive = true;
    }

    // order of providers
    // first one is default, the rest is for display order on login form
    final Map<String, Map<String, Object>> configs = new LinkedHashMap<>();

    final Consumer<String> addConfig =
        name -> {
          Map<String, Object> config = initConfigs.remove(name);
          if (config == null) {
            config =
                Optional.ofNullable(CONFIGS.get(name))
                    .orElseThrow(() -> new NoSuchElementException(name))
                    .toMap();
          }
          configs.put(name, config);
        };

    final List<String> authOrder = settings.getList(AvailableAppSettings.AUTH_ORDER);
    if (ObjectUtils.notEmpty(authOrder)) {
      authOrder.forEach(addConfig::accept);
    }

    configs.putAll(initConfigs);
    initConfigs.clear();

    final Map<String, Client> configuredClients =
        configs.entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e -> createClient(e.getKey(), e.getValue()),
                    (oldValue, newValue) -> oldValue,
                    LinkedHashMap::new));

    final String authDefault =
        settings.get(AvailableAppSettings.AUTH_DEFAULT, exclusive ? null : "form");
    final Client defaultClient =
        StringUtils.notBlank(authDefault)
            ? Optional.ofNullable(configuredClients.get(authDefault))
                .orElseThrow(() -> new NoSuchElementException(authDefault))
            : configuredClients.values().iterator().next();

    defaultClientName = defaultClient.getName();
    clients.add(defaultClient);
    configuredClients.values().stream()
        .filter(client -> client != defaultClient)
        .forEach(clients::add);

    settings
        .getList(AvailableAppSettings.AUTH_LOCAL_BASIC_AUTH)
        .forEach(
            name -> {
              switch (name.toLowerCase()) {
                case "indirect":
                  clients.add(Beans.get(IndirectBasicAuthClient.class));
                  break;
                case "direct":
                  clients.add(Beans.get(DirectBasicAuthClient.class));
                  break;
                default:
                  throw new IllegalArgumentException("Invalid basic auth client: " + name);
              }
            });

    // set titles and icons
    final Iterator<Map<String, Object>> configIt = configs.values().iterator();
    final Iterator<Client> clientIt = configuredClients.values().iterator();
    while (configIt.hasNext() && clientIt.hasNext()) {
      final Map<String, Object> props = configIt.next();
      final Client client = clientIt.next();
      if (!(client instanceof IndirectClient)) {
        continue;
      }
      final String name = client.getName();
      final String title = (String) props.getOrDefault("title", name);
      final String icon = (String) props.get("icon");
      final Map<String, String> info = new HashMap<>();
      info.put("title", title);
      if (StringUtils.notBlank(icon)) {
        info.put("icon", icon);
      }
      authPac4jInfo.setClientInfo(name, info);
    }

    logger.info("Default client: {}", defaultClientName);

    final Map<Boolean, List<Client>> groupedClients =
        clients.stream().collect(Collectors.groupingBy(IndirectClient.class::isInstance));
    indirectClientNames =
        Collections.unmodifiableSet(
            (Set<String>)
                groupedClients.getOrDefault(true, Collections.emptyList()).stream()
                    .map(Client::getName)
                    .collect(Collectors.toCollection(LinkedHashSet::new)));
    if (!indirectClientNames.isEmpty()) {
      logger.info("Indirect clients: {}", indirectClientNames);
    }
    directClientNames =
        Collections.unmodifiableSet(
            (Set<String>)
                groupedClients.getOrDefault(false, Collections.emptyList()).stream()
                    .map(Client::getName)
                    .collect(Collectors.toCollection(LinkedHashSet::new)));
    if (!directClientNames.isEmpty()) {
      logger.info("Direct clients: {}", directClientNames);
    }
  }

  @Override
  public List<Client> get() {
    return clients;
  }

  public String getDefaultClientName() {
    return defaultClientName;
  }

  public Set<String> getIndirectClientNames() {
    return indirectClientNames;
  }

  public Set<String> getDirectClientNames() {
    return directClientNames;
  }

  public boolean isExclusive() {
    return exclusive;
  }

  private Client createClient(String name, Map<String, Object> props) {
    final String clientClassName = (String) props.get("client");

    if (clientClassName == null) {
      throw new RuntimeException(
          String.format("Must specify client with custom provider: %s", name));
    }

    final Class<?> clientClass = findClass(clientClassName);
    final Client client = (Client) inject(clientClass);

    if (client instanceof IndirectClient) {
      final IndirectClient indirectClient = (IndirectClient) client;
      indirectClient.setUrlResolver(new AxelorUrlResolver());
    }

    final String configClassName = (String) props.get("configuration");
    final Class<?> configClass;
    final Object config;

    if (configClassName != null) {
      configClass = findClass(configClassName);
    } else {
      configClass = findConfigurationClass(clientClass);
    }

    if (configClass != null) {
      config = inject(configClass);
      setField(client, "configuration", config);
    } else {
      config = null;
    }

    final Consumer<Entry<String, Object>> configurer =
        config != null
            ? item -> setConfig(client, config, item.getKey(), item.getValue())
            : item -> setField(client, item.getKey(), item.getValue());

    props.entrySet().stream()
        .filter(item -> !EXTRA_CONFIGS.contains(item.getKey()))
        .forEach(configurer);

    postprocessClient(client, config);

    return client;
  }

  private void postprocessClient(Client client, Object config) {

    // Remove private_key_jwt from supported authentication methods
    // if private key JWT config is missing.
    if (config instanceof OidcConfiguration) {
      final OidcConfiguration oidcConfig = (OidcConfiguration) config;
      final PrivateKeyJWTClientAuthnMethodConfig privateKeyJwtConfig =
          oidcConfig.getPrivateKeyJWTClientAuthnMethodConfig();
      if (privateKeyJwtConfig != null) {
        return;
      }

      final ClientAuthenticationMethod clientAuthenticationMethod =
          oidcConfig.getClientAuthenticationMethod();
      if (ClientAuthenticationMethod.PRIVATE_KEY_JWT.equals(clientAuthenticationMethod)) {
        logger.error(
            "{}: {} is specified as client authentication method, "
                + "but privateKeyJWTClientAuthnMethodConfig is missing.",
            client.getName(),
            ClientAuthenticationMethod.PRIVATE_KEY_JWT);
      }

      final Set<ClientAuthenticationMethod> supportedMethods =
          oidcConfig.getSupportedClientAuthenticationMethods();
      if (supportedMethods == null) {
        final Set<ClientAuthenticationMethod> actualSupportedMethods =
            new HashSet<>(
                Arrays.asList(
                    ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                    ClientAuthenticationMethod.CLIENT_SECRET_POST,
                    ClientAuthenticationMethod.NONE));
        oidcConfig.setSupportedClientAuthenticationMethods(actualSupportedMethods);
      } else if (supportedMethods.contains(ClientAuthenticationMethod.PRIVATE_KEY_JWT)) {
        logger.error(
            "{}: {} is specified as supported client authentication method, "
                + "but privateKeyJWTClientAuthnMethodConfig is missing.",
            client.getName(),
            ClientAuthenticationMethod.PRIVATE_KEY_JWT);
      }
    }
  }

  @Nullable
  private Class<?> findConfigurationClass(Class<?> clientClass) {
    final Class<?> baseConfigClass;

    try {
      final Method getter = findGetter(clientClass, "configuration");
      baseConfigClass = getter.getReturnType();
    } catch (NoSuchMethodException e) {
      return null;
    }

    // Find specialized configuration class from constructors if any.
    return Stream.of(clientClass.getConstructors())
        .filter(constructor -> constructor.getParameterCount() == 1)
        .<Class<?>>map(constructor -> constructor.getParameterTypes()[0])
        .filter(baseConfigClass::isAssignableFrom)
        .findFirst()
        .orElse(baseConfigClass);
  }

  private void setConfig(Object client, Object config, String property, Object value) {
    Exception configError = null;

    try {
      setFieldChecked(config, property, value);
    } catch (NoSuchMethodException e) {
      configError = e;
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }

    // always try to set on client in case of duplicate property
    // see `scope` on both GenericOAuth20Client and OAuthConfiguration

    try {
      setFieldChecked(client, property, value);
    } catch (ReflectiveOperationException e) {
      // don't throw exception if config has been set
      if (configError != null) {
        throw new RuntimeException(configError);
      }
    }
  }

  private void setField(Object obj, String property, Object value) {
    try {
      setFieldChecked(obj, property, value);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  private void setFieldChecked(Object obj, String property, Object value)
      throws ReflectiveOperationException {
    final Method setter = findSetter(obj.getClass(), property);
    Class<?> type = setter.getParameterTypes()[0];
    type = PRIMITIVE_TYPES.getOrDefault(type, type);
    final Type genericType = setter.getGenericParameterTypes()[0];
    final Object converted = convert(value, type, genericType);
    setter.invoke(obj, converted);
  }

  private Object convert(Object value, Class<?> type, Type genericType) {
    final String valueStr = String.valueOf(value);

    if (Collection.class.isAssignableFrom(type)) {
      List<?> items = Arrays.asList(valueStr.split("\\s*,\\s*"));
      if (genericType instanceof ParameterizedType) {
        final Class<?> itemType =
            (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];
        items =
            items.stream().map(item -> convert(item, itemType, null)).collect(Collectors.toList());
      }
      if (type.isAssignableFrom(List.class)) {
        return items;
      }
      if (type.isAssignableFrom(Set.class)) {
        return new HashSet<>(items);
      }
      value = items;
    }

    if (type.isAssignableFrom(value.getClass())) {
      return value;
    }

    final UnaryOperator<Object> converter = CONVERTERS.get(type);
    if (converter != null) {
      return converter.apply(value);
    }

    for (final String name : List.of("valueOf", "parse")) {
      try {
        final Method converterMethod = type.getMethod(name, String.class);
        return converterMethod.invoke(null, valueStr);
      } catch (NoSuchMethodException e) {
        // Ignore
      } catch (ReflectiveOperationException e) {
        throw new RuntimeException(e);
      }
    }

    try {
      final Class<?> cls = Class.forName(valueStr);
      return Beans.get(cls);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  private static Map<String, Object> getCamelizedMapKeys(Map<String, Object> map) {
    final Map<String, Object> result = new HashMap<>();
    final Inflector inflector = Inflector.getInstance();
    for (final Map.Entry<String, Object> entry : map.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      result.put(inflector.camelize(key, true), value);
    }
    return result;
  }

  private Method findGetter(Class<?> klass, String property) throws NoSuchMethodException {
    final String getter = "get" + Inflector.getInstance().camelize(property);
    final List<Method> getterMethods =
        Stream.of(klass.getMethods())
            .filter(m -> m.getName().equalsIgnoreCase(getter))
            .filter(m -> m.getParameterCount() == 0)
            .collect(Collectors.toList());

    if (getterMethods.isEmpty()) {
      throw new NoSuchMethodException(String.format("%s.%s", klass.getName(), getter));
    }

    Method result = getterMethods.get(0);
    for (final Method method : getterMethods.subList(1, getterMethods.size())) {
      if (result.getReturnType().isAssignableFrom(method.getReturnType())) {
        result = method;
      }
    }

    return result;
  }

  private Method findSetter(Class<?> klass, String property) throws NoSuchMethodException {
    final String setter = "set" + Inflector.getInstance().camelize(property);
    return Stream.of(klass.getMethods())
        .filter(m -> m.getName().equalsIgnoreCase(setter))
        .filter(m -> m.getParameterCount() == 1)
        .findAny()
        .orElseThrow(
            () -> new NoSuchMethodException(String.format("%s.%s", klass.getName(), setter)));
  }

  private Class<?> findClass(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private Object inject(Class<?> klass) {
    try {
      return Beans.get(klass);
    } catch (Exception e) {
      logger.warn("Injection of {} failed: {}", klass, e);
      return newInstance(klass);
    }
  }

  private Object newInstance(Class<?> klass) {
    try {
      return klass.getConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }
}
