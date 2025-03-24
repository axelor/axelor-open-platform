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

import com.axelor.cache.caffeine.CaffeineCacheBuilder;
import com.axelor.cache.redisson.AxelorRedissonRegionFactory;
import com.axelor.cache.redisson.AxelorRedissonRegionNativeFactory;
import com.axelor.cache.redisson.RedissonCacheBuilder;
import com.axelor.cache.redisson.RedissonCacheNativeBuilder;
import com.axelor.common.Inflector;
import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider;
import java.util.function.Function;
import javax.cache.spi.CachingProvider;

public enum CacheType {
  CAFFEINE(name -> new CaffeineCacheBuilder<>(name), CaffeineCachingProvider.class, "jcache"),
  REDISSON(
      name -> new RedissonCacheBuilder<>(name),
      org.redisson.jcache.JCachingProvider.class,
      AxelorRedissonRegionFactory.class.getName()),
  REDISSON_NATIVE(
      name -> new RedissonCacheNativeBuilder<>(name),
      org.redisson.jcache.JCachingProvider.class,
      AxelorRedissonRegionNativeFactory.class.getName());

  private final Function<String, CacheBuilder<?, ?>> cacheBuilderFactory;
  private final Class<? extends CachingProvider> cachingProviderClass;
  private final String cacheRegionFactory;

  CacheType(
      Function<String, CacheBuilder<?, ?>> cacheBuilderFactory,
      Class<? extends CachingProvider> cachingProviderClass,
      String cacheRegionFactory) {
    this.cacheBuilderFactory = cacheBuilderFactory;
    this.cachingProviderClass = cachingProviderClass;
    this.cacheRegionFactory = cacheRegionFactory;
  }

  public String getName() {
    var inflector = Inflector.getInstance();
    return inflector.dasherize(name().toLowerCase());
  }

  @SuppressWarnings("rawtypes")
  public CacheBuilder getCacheBuilder(String cacheName) {
    return cacheBuilderFactory.apply(cacheName);
  }

  public Class<? extends CachingProvider> getCachingProviderClass() {
    return cachingProviderClass;
  }

  public String getCacheRegionFactory() {
    return cacheRegionFactory;
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
