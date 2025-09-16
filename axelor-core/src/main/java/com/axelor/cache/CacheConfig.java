/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.StringUtils;
import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Cache configuration
 *
 * <p>This is composed of static methods, as it can be used before Guice injector is initialized.
 */
public class CacheConfig {

  public static final String DEFAULT_JCACHE_PROVIDER = CaffeineCachingProvider.class.getName();

  private static Predicate<? super String> filterDefault =
      name -> !"default".equalsIgnoreCase(name);

  private CacheConfig() {}

  public static Optional<CacheProviderInfo> getAppCacheProvider() {
    return getSetting(AvailableAppSettings.APPLICATION_CACHE_PROVIDER)
        .map(
            provider ->
                new CacheProviderInfo(
                    provider,
                    getSettings(AvailableAppSettings.APPLICATION_CACHE_CONFIG_PREFIX),
                    AvailableAppSettings.APPLICATION_CACHE_CONFIG_PREFIX));
  }

  public static Optional<CacheProviderInfo> getHibernateCacheProvider() {
    return getSetting(AvailableAppSettings.APPLICATION_CACHE_HIBERNATE_PROVIDER)
        .or(() -> getSetting(AvailableAppSettings.APPLICATION_CACHE_PROVIDER))
        .map(
            provider -> {
              var prefix = AvailableAppSettings.APPLICATION_CACHE_HIBERNATE_CONFIG_PREFIX;
              var config = getSettings(prefix);

              if (config.isEmpty()) {
                prefix = AvailableAppSettings.APPLICATION_CACHE_CONFIG_PREFIX;
                config = getSettings(prefix);
              }

              return new CacheProviderInfo(provider, config, prefix);
            });
  }

  public static Optional<CacheProviderInfo> getShiroCacheProvider() {
    return getSetting(AvailableAppSettings.APPLICATION_CACHE_SHIRO_PROVIDER)
        .or(() -> getSetting(AvailableAppSettings.APPLICATION_CACHE_PROVIDER))
        .map(
            provider -> {
              var prefix = AvailableAppSettings.APPLICATION_CACHE_SHIRO_CONFIG_PREFIX;
              var config = getSettings(prefix);

              if (config.isEmpty()) {
                prefix = AvailableAppSettings.APPLICATION_CACHE_CONFIG_PREFIX;
                config = getSettings(prefix);
              }

              return new CacheProviderInfo(provider, config, prefix);
            });
  }

  protected static Optional<String> getSetting(String key) {
    return Optional.ofNullable(AppSettings.get().get(key))
        .filter(StringUtils::notBlank)
        .filter(filterDefault);
  }

  protected static Map<String, String> getSettings(String keyPrefix) {
    return stripKeyPrefix(AppSettings.get().getPropertiesStartingWith(keyPrefix), keyPrefix);
  }

  private static <V> Map<String, V> stripKeyPrefix(Map<String, V> map, String prefix) {
    return map.entrySet().stream()
        .collect(
            Collectors.toUnmodifiableMap(
                entry -> entry.getKey().substring(prefix.length()), Entry::getValue));
  }
}
