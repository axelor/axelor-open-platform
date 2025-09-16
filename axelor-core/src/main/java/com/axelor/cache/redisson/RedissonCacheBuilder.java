/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.redisson;

import com.axelor.cache.AxelorCache;
import com.axelor.cache.CacheBuilder;
import com.axelor.cache.event.RemovalCause;
import org.redisson.api.RMapCache;
import org.redisson.api.map.event.EntryEvent;
import org.redisson.api.map.event.EntryExpiredListener;
import org.redisson.api.map.event.EntryRemovedListener;
import org.redisson.api.map.event.EntryUpdatedListener;
import org.redisson.api.options.MapCacheOptions;

/**
 * Redisson cache builder
 *
 * <p>This builds an {@link AxelorCache} wrapping a {@link org.redisson.api.RMapCache}.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class RedissonCacheBuilder<K, V>
    extends AbstractRedissonCacheBuilder<K, V, RMapCache<K, V>, MapCacheOptions<K, V>> {

  public RedissonCacheBuilder(String cacheName) {
    super(cacheName);
  }

  public RedissonCacheBuilder(CacheBuilder<K, V> builder) {
    super(builder);
  }

  @Override
  protected MapCacheOptions<K, V> newOptions() {
    return MapCacheOptions.<K, V>name(getCacheName());
  }

  @Override
  protected RMapCache<K, V> newMapCache(MapCacheOptions<K, V> options) {
    return RedissonProvider.get().getMapCache(options);
  }

  @Override
  protected RedissonCache<K, V> newRedissonCache(RMapCache<K, V> cache) {
    return new RedissonCache<>(cache);
  }

  @Override
  protected void configureCache(AbstractRedissonCache<K, V, RMapCache<K, V>> cache) {
    super.configureCache(cache);
    var redissonCache = (RedissonCache<K, V>) cache;
    var removalListener = getRemovalListener();

    if (removalListener != null) {
      redissonCache.addListener(
          (EntryRemovedListener<K, V>)
              event ->
                  removalListener.onRemoval(
                      event.getKey(), event.getOldValue(), toRemovalCause(event)));

      redissonCache.addListener(
          (EntryUpdatedListener<K, V>)
              event ->
                  removalListener.onRemoval(
                      event.getKey(), event.getOldValue(), toRemovalCause(event)));

      redissonCache.addListener(
          (EntryExpiredListener<K, V>)
              event ->
                  removalListener.onRemoval(
                      event.getKey(), event.getValue(), toRemovalCause(event)));
    }
  }

  protected RemovalCause toRemovalCause(EntryEvent<K, V> event) {
    return switch (event.getType()) {
      case REMOVED -> RemovalCause.REMOVED;
      case UPDATED -> RemovalCause.REPLACED;
      case EXPIRED -> RemovalCause.EXPIRED;
      case CREATED -> throw new UnsupportedOperationException();
    };
  }
}
