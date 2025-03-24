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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import javax.cache.spi.CachingProvider;

/**
 * Cache provider information
 *
 * <p>This is used to store the cache provider name and configuration.
 */
public class CacheProviderInfo {

  private final String provider;
  private final Map<String, String> config;
  private final String configPrefix;

  public CacheProviderInfo(String provider, Map<String, String> config, String configPrefix) {
    this.provider = provider;
    this.config = config;
    this.configPrefix = configPrefix;
  }

  public CacheProviderInfo(String provider) {
    this(provider, Collections.emptyMap(), "");
  }

  public String getProvider() {
    return provider;
  }

  public Map<String, String> getConfig() {
    return config;
  }

  public String getConfigPrefix() {
    return configPrefix;
  }

  public Class<? extends CachingProvider> getCachingProvider() {
    var cacheType = getCacheType();

    if (cacheType.isPresent()) {
      return cacheType.get().getCachingProviderClass();
    }

    var providerName = getProvider();
    Class<? extends CachingProvider> providerClass;
    try {
      providerClass = Class.forName(providerName).asSubclass(CachingProvider.class);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Unknown cache provider: " + providerName);
    } catch (ClassCastException e) {
      throw new IllegalArgumentException("Invalid cache provider type: " + providerName);
    }

    if (!CachingProvider.class.isAssignableFrom(providerClass)) {
      throw new IllegalArgumentException("Unsupported cache provider type: " + providerName);
    }

    return providerClass;
  }

  public Optional<CacheType> getCacheType() {
    try {
      return Optional.of(CacheType.from(provider));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
