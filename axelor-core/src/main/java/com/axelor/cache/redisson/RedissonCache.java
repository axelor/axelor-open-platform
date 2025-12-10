/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.redisson;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RMapCache;
import org.redisson.api.map.event.MapEntryListener;

/**
 * Redisson cache with scripted eviction
 *
 * <p>This wraps a {@link org.redisson.api.RMapCache}.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class RedissonCache<K, V> extends AbstractRedissonCache<K, V, RMapCache<K, V>> {

  private volatile long ttl;
  private static final TimeUnit TTL_UNIT = TimeUnit.MILLISECONDS;

  private volatile long maxIdleTime;
  private static final TimeUnit MAX_IDLE_TIME_UNIT = TimeUnit.MILLISECONDS;

  private final Set<Integer> listenerIds = ConcurrentHashMap.newKeySet();

  public RedissonCache(RMapCache<K, V> cache) {
    super(cache);
  }

  @Override
  public void setExpireAfterWrite(Duration expireAfterWrite) {
    ttl = expireAfterWrite.toMillis();
  }

  @Override
  public void setExpireAfterAccess(Duration expireAfterAccess) {
    maxIdleTime = expireAfterAccess.toMillis();
  }

  @Override
  public void setMaximumSize(int maximumSize) {
    cache.setMaxSize(maximumSize);
  }

  @Override
  public void put(K key, V value) {
    cache.fastPut(key, value, ttl, TTL_UNIT, maxIdleTime, MAX_IDLE_TIME_UNIT);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    if (maxIdleTime != 0) {
      // Iteration to ensure maxIdle and TTL are applied on each entry
      map.forEach(this::put);
    } else if (ttl != 0) {
      // Scripted Bulk to ensure TTL is applied on each entry
      cache.putAll(map, ttl, TTL_UNIT);
    } else {
      // Native Bulk (No expiration)
      cache.putAll(map);
    }
  }

  @Override
  public void close() {
    if (!listenerIds.isEmpty()) {
      listenerIds.forEach(cache::removeListener);
      listenerIds.clear();
    }

    super.close();
  }

  public int addListener(MapEntryListener listener) {
    var listenerId = cache.addListener(listener);
    listenerIds.add(listenerId);

    return listenerId;
  }

  public void removeListener(int listenerId) {
    cache.removeListener(listenerId);
    listenerIds.remove(listenerId);
  }
}
