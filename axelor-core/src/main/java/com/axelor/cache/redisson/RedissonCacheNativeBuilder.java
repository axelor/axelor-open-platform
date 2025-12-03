/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.redisson;

import com.axelor.cache.AxelorCache;
import com.axelor.cache.CacheBuilder;
import com.axelor.cache.event.RemovalListener;
import org.redisson.api.RMapCacheNative;
import org.redisson.api.options.MapOptions;

/**
 * Redisson cache with native eviction builder
 *
 * <p>This builds an {@link AxelorCache} wrapping a {@link org.redisson.api.RMapCacheNative}.
 *
 * <p>Weak references are not supported in Redisson collections. When either {@code weakKeys} or
 * {@code weakValues} are used, TTL is set in order to approximate the behavior.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class RedissonCacheNativeBuilder<K, V>
    extends AbstractRedissonCacheBuilder<K, V, RMapCacheNative<K, V>, MapOptions<K, V>> {

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

  /**
   * Not supported with native eviction. When removal listener is set, fall back to {@link
   * RedissonCacheBuilder}.
   */
  @Override
  public <K1 extends K, V1 extends V> CacheBuilder<K1, V1> removalListener(
      RemovalListener<? super K1, ? super V1> removalListener) {
    super.removalListener(removalListener);

    @SuppressWarnings("unchecked")
    var self = (CacheBuilder<K1, V1>) this;

    return removalListener != null ? new RedissonCacheBuilder<>(self) : self;
  }
}
