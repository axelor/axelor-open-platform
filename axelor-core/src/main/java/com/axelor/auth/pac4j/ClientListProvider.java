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
package com.axelor.auth.pac4j;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.Inflector;
import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.inject.Beans;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Provider;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.http.url.DefaultUrlResolver;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.http.client.indirect.IndirectBasicAuthClient;
import org.pac4j.ldap.profile.service.LdapProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ClientListProvider implements Provider<List<Client>> {

  private final List<Client> clients = new ArrayList<>();

  private final boolean exclusive;

  // Default client configurations
  private static final Map<String, ClientConfig> CONFIGS =
      new ImmutableMap.Builder<String, ClientConfig>()
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
                  .requiresAbsoluteUrl()
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
      Map.of(
          "exclusive",
          (settings, key) -> settings.getBoolean(key, false),
          "absoluteUrlRequired",
          (settings, key) -> settings.getBoolean(key, false));

  private static final Set<String> EXTRA_CONFIGS =
      Set.of("client", "configuration", "title", "icon", "exclusive", "absoluteUrlRequired");

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
        String config = Inflector.getInstance().camelize(matcher.group("config"), true);
        Object value =
            SETTINGS_GETTERS.getOrDefault(config, (s, k) -> s.get(k, null)).apply(settings, key);
        initConfigs.computeIfAbsent(name, k -> new HashMap<>()).put(config, value);
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
              props.computeIfAbsent("client", k -> config.getClient());
              props.computeIfAbsent("configuration", k -> config.getConfiguration());
              props.computeIfAbsent("title", k -> config.getTitle());
              props.computeIfAbsent("icon", k -> config.getIcon());
              props.computeIfAbsent("exclusive", k -> config.isExclusive());
              props.computeIfAbsent("absoluteUrlRequired", k -> config.isAbsoluteUrlRequired());
            });

    // order of providers displayed on login form
    final Map<String, Map<String, Object>> configs;
    final List<String> authOrder = settings.getList(AvailableAppSettings.AUTH_ORDER);
    if (ObjectUtils.isEmpty(authOrder)) {
      configs = initConfigs;
    } else {
      configs = new LinkedHashMap<>();
      authOrder.forEach(
          name -> {
            final Map<String, Object> config = initConfigs.remove(name);
            if (config != null) {
              configs.put(name, config);
            }
          });
      configs.putAll(initConfigs);
    }

    final List<Client> centralClients =
        configs.entrySet().stream()
            .map(e -> createClient(e.getKey(), e.getValue()))
            .map(Client.class::cast)
            .collect(Collectors.toList());

    // check for exclusive clients
    if (configs.isEmpty()
        || configs.size() > 1
        || !((boolean) configs.values().iterator().next().getOrDefault("exclusive", false))) {
      clients.add(Beans.get(FormClient.class));
      exclusive = false;
    } else {
      exclusive = true;
    }

    // check for clients requiring absolute callback URL
    if (configs.values().stream()
        .anyMatch(props -> (boolean) props.getOrDefault("absoluteUrlRequired", false))) {
      authPac4jInfo.requireAbsoluteUrl();
    }

    clients.addAll(centralClients);

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
    final Iterator<Client> clientIt = centralClients.iterator();
    while (configIt.hasNext() && clientIt.hasNext()) {
      final Map<String, Object> props = configIt.next();
      final String name = clientIt.next().getName();
      final String title = (String) props.getOrDefault("title", name);
      final String icon = (String) props.get("icon");
      final Map<String, String> info = new HashMap<>();
      info.put("title", title);
      if (StringUtils.notBlank(icon)) {
        info.put("icon", icon);
      }
      authPac4jInfo.setClientInfo(name, info);
    }

    if (logger.isInfoEnabled()) {
      final String clientNames =
          clients.stream().map(Client::getName).collect(Collectors.joining(", "));
      logger.info("Clients: {}", clientNames);
    }
  }

  @Override
  public List<Client> get() {
    return clients;
  }

  public boolean isExclusive() {
    return exclusive;
  }

  private Object createClient(String name, Map<String, Object> props) {
    final String clientClassName = (String) props.get("client");

    if (clientClassName == null) {
      throw new RuntimeException(String.format("Unsupported provider: %s", name));
    }

    final Class<?> clientClass = findClass(clientClassName);
    final Object client = inject(clientClass);

    if (client instanceof IndirectClient) {
      final IndirectClient indirectClient = (IndirectClient) client;
      indirectClient.setUrlResolver(new DefaultUrlResolver(true));
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

    return client;
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
    final Exception configError;

    try {
      setFieldChecked(config, property, value);
      return;
    } catch (NoSuchMethodException e) {
      configError = e;
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }

    try {
      setFieldChecked(client, property, value);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(configError);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
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
    final List<String> propertyParts = Arrays.asList(property.split("\\.", 2));
    final Method setter = findSetter(obj.getClass(), propertyParts.get(0));
    Class<?> type = setter.getParameterTypes()[0];
    type = PRIMITIVE_TYPES.getOrDefault(type, type);

    if (type.isAssignableFrom(Map.class)) {
      final Method getter = findGetter(obj.getClass(), propertyParts.get(0));
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) getter.invoke(obj);
      if (map == null) {
        map = new LinkedHashMap<>();
        setter.invoke(obj, map);
      }
      map.put(propertyParts.get(1), value);
      return;
    }

    final Object converted = convert(type, value);
    setter.invoke(obj, converted);
  }

  private Object convert(Class<?> type, Object value) throws ReflectiveOperationException {
    if (type.isAssignableFrom(value.getClass())) {
      return value;
    }
    final String valueStr = String.valueOf(value);
    if (type.isAssignableFrom(List.class)) {
      return Arrays.asList(valueStr.split("\\s*,\\s*"));
    }
    try {
      final Method valueOf = type.getMethod("valueOf", String.class);
      return valueOf.invoke(null, valueStr);
    } catch (NoSuchMethodException e) {
      final Class<?> cls = Class.forName(valueStr);
      return Beans.get(cls);
    }
  }

  private Method findGetter(Class<?> klass, String property) throws NoSuchMethodException {
    final String getter = "get" + Inflector.getInstance().camelize(property);
    return Stream.of(klass.getMethods())
        .filter(m -> m.getName().equalsIgnoreCase(getter))
        .filter(m -> m.getParameterCount() == 0)
        .findAny()
        .orElseThrow(
            () -> new NoSuchMethodException(String.format("%s.%s", klass.getName(), getter)));
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
