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
  protected MapOptions<K, V> newOptions() {
    return MapOptions.<K, V>name(getCacheName());
  }

  @Override
  protected RMapCacheNative<K, V> newMapCache(MapOptions<K, V> options) {
    return getRedissonClient().getMapCacheNative(options);
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
  public CacheBuilder<K, V> removalListener(RemovalListener<K, V> removalListener) {
    super.removalListener(removalListener);
    return removalListener != null ? new RedissonCacheBuilder<>(this) : this;
  }
}
