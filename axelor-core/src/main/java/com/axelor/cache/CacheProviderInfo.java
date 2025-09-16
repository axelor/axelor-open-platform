/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
