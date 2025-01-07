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

import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider;
import java.util.Optional;
import javax.cache.spi.CachingProvider;

/**
 * Cache provider information
 *
 * <p>This is used to store the cache provider name and configuration.
 */
public class CacheProviderInfo {

  private final String provider;
  private final Optional<String> config;

  public CacheProviderInfo(String provider, Optional<String> config) {
    this.provider = provider;
    this.config = config;
  }

  public CacheProviderInfo(String provider) {
    this.provider = provider;
    this.config = Optional.empty();
  }

  public String getProvider() {
    return provider;
  }

  public Optional<String> getConfig() {
    return config;
  }

  public Class<? extends CachingProvider> getCachingProvider() {
    var providerName = getProvider();

    if ("caffeine".equalsIgnoreCase(providerName)) {
      return CaffeineCachingProvider.class;
    } else if ("redisson".equalsIgnoreCase(providerName)) {
      return org.redisson.jcache.JCachingProvider.class;
    }

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
}
