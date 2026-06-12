/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.redisson;

import com.axelor.cache.AxelorCache;
import com.axelor.cache.CacheBuilder;
import com.axelor.cache.CacheLoader;
import com.axelor.cache.event.RemovalListener;
import org.redisson.api.RMapCacheNative;
import org.redisson.api.options.MapOptions;

/**
 * Redisson cache with native eviction builder
 *
 * <p>This builds an {@link AxelorCache} wrapping a {@link org.redisson.api.RMapCacheNative}.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class RedissonCacheNativeBuilder<K, V>
    extends AbstractRedissonCacheBuilder<
        K, V, RedissonCacheNativeBuilder<K, V>, RMapCacheNative<K, V>, MapOptions<K, V>> {

  public RedissonCacheNativeBuilder(String cacheName) {
    super(cacheName);
  }

  @Override
  protected MapOptions<K, V> newOptions(String name) {
    return MapOptions.<K, V>name(name);
  }

  @Override
  protected RMapCacheNative<K, V> newMapCache(MapOptions<K, V> options) {
    return RedissonProvider.get().getMapCacheNative(options);
  }

  @Override
  protected RedissonCacheNative<K, V> newRedissonCache(RMapCacheNative<K, V> cache) {
    return new RedissonCacheNative<>(cache);
  }

  @Override
  protected AbstractRedissonCache<K, V, RMapCacheNative<K, V>> newRedissonCache(
      RMapCacheNative<K, V> cache, CacheLoader<K, V> loader) {
    return new RedissonLoadingCacheNative<>(cache, loader);
  }

  /**
   * Not supported with native eviction. When removal listener is set, fall back to {@link
   * RedissonCacheBuilder}.
   */
  @SuppressWarnings({"unchecked"})
  @Override
  public <K1 extends K, V1 extends V, B1 extends CacheBuilder<K1, V1, B1>>
      CacheBuilder<K1, V1, B1> removalListener(
          RemovalListener<? super K1, ? super V1> removalListener) {
    super.removalListener(removalListener);

    var self = (CacheBuilder<K1, V1, B1>) this;

    return (removalListener != null
        ? (CacheBuilder<K1, V1, B1>) new RedissonCacheBuilder<>(self)
        : self);
  }
}
