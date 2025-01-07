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
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Cache configuration
 *
 * <p>This is composed of static methods, as it can be used before Guice injector is initialized.
 */
public class CacheConfig {

  private static Predicate<? super String> filterDefault =
      name -> !"default".equalsIgnoreCase(name);

  private CacheConfig() {}

  public static Optional<CacheProviderInfo> getAppCacheProvider() {
    return getSetting(AvailableAppSettings.APPLICATION_CACHE_PROVIDER)
        .map(
            provider ->
                new CacheProviderInfo(
                    provider, getSetting(AvailableAppSettings.APPLICATION_CACHE_CONFIG)));
  }

  public static Optional<CacheProviderInfo> getHibernateCacheProvider() {
    return getSetting(AvailableAppSettings.APPLICATION_CACHE_HIBERNATE_PROVIDER)
        .or(() -> getSetting(AvailableAppSettings.APPLICATION_CACHE_PROVIDER))
        .map(
            provider ->
                new CacheProviderInfo(
                    provider,
                    getSetting(AvailableAppSettings.APPLICATION_CACHE_HIBERNATE_CONFIG)
                        .or(() -> getSetting(AvailableAppSettings.APPLICATION_CACHE_CONFIG))));
  }

  public static Optional<CacheProviderInfo> getShiroCacheProvider() {
    return getSetting(AvailableAppSettings.APPLICATION_CACHE_SHIRO_PROVIDER)
        .or(() -> getSetting(AvailableAppSettings.APPLICATION_CACHE_PROVIDER))
        .map(
            provider ->
                new CacheProviderInfo(
                    provider,
                    getSetting(AvailableAppSettings.APPLICATION_CACHE_SHIRO_CONFIG)
                        .or(() -> getSetting(AvailableAppSettings.APPLICATION_CACHE_CONFIG))));
  }

  protected static Optional<String> getSetting(String key) {
    return Optional.ofNullable(AppSettings.get().get(key))
        .filter(StringUtils::notBlank)
        .filter(filterDefault);
  }
}
