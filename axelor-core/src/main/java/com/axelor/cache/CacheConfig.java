/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
