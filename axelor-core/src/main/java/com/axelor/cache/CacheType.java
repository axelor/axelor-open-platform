/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache;

import com.axelor.cache.caffeine.CaffeineCacheBuilder;
import com.axelor.cache.caffeine.CaffeineDistributedService;
import com.axelor.cache.redisson.AxelorRedissonRegionFactory;
import com.axelor.cache.redisson.AxelorRedissonRegionNativeFactory;
import com.axelor.cache.redisson.RedissonCacheBuilder;
import com.axelor.cache.redisson.RedissonCacheNativeBuilder;
import com.axelor.cache.redisson.RedissonDistributedService;
import com.axelor.common.Inflector;
import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider;
import java.util.function.Function;
import javax.cache.spi.CachingProvider;

public enum CacheType {
  CAFFEINE(
      name -> new CaffeineCacheBuilder<>(name),
      CaffeineCachingProvider.class,
      "jcache",
      new CaffeineDistributedService(),
      false),
  REDISSON(
      name -> new RedissonCacheBuilder<>(name),
      org.redisson.jcache.JCachingProvider.class,
      AxelorRedissonRegionFactory.class.getName(),
      new RedissonDistributedService(),
      true),
  REDISSON_NATIVE(
      name -> new RedissonCacheNativeBuilder<>(name),
      org.redisson.jcache.JCachingProvider.class,
      AxelorRedissonRegionNativeFactory.class.getName(),
      new RedissonDistributedService(),
      true);

  private final Function<String, CacheBuilder<?, ?>> cacheBuilderFactory;
  private final Class<? extends CachingProvider> cachingProviderClass;
  private final String cacheRegionFactory;
  private final DistributedService distributedService;
  private final boolean distributed;

  CacheType(
      Function<String, CacheBuilder<?, ?>> cacheBuilderFactory,
      Class<? extends CachingProvider> cachingProviderClass,
      String cacheRegionFactory,
      DistributedService distributedService,
      boolean distributed) {
    this.cacheBuilderFactory = cacheBuilderFactory;
    this.cachingProviderClass = cachingProviderClass;
    this.cacheRegionFactory = cacheRegionFactory;
    this.distributedService = distributedService;
    this.distributed = distributed;
  }

  public String getName() {
    var inflector = Inflector.getInstance();
    return inflector.dasherize(name().toLowerCase());
  }

  @SuppressWarnings("rawtypes")
  public CacheBuilder getCacheBuilder(String cacheName) {
    return cacheBuilderFactory.apply(cacheName);
  }

  public DistributedService getDistributedService() {
    return distributedService;
  }

  public Class<? extends CachingProvider> getCachingProviderClass() {
    return cachingProviderClass;
  }

  public String getCacheRegionFactory() {
    return cacheRegionFactory;
  }

  public boolean isDistributed() {
    return distributed;
  }

  /**
   * Converts a provider name to a {@link CacheType}.
   *
   * @param name provider name
   * @return {@link CacheType}
   * @throws IllegalArgumentException if the name is not a valid cache provider
   */
  public static CacheType from(String name) {
    return CacheType.valueOf(name.toUpperCase().replace("-", "_"));
  }
}
