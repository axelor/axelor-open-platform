/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.redisson;

import java.time.Duration;
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

  private long ttl;
  private TimeUnit ttlUnit;

  private long maxIdleTime;
  private TimeUnit maxIdleTimeUnit;

  private CachePutter<K, V> putter;

  private final Set<Integer> listenerIds = ConcurrentHashMap.newKeySet();

  public RedissonCache(RMapCache<K, V> cache) {
    super(cache);
    updatePutter();
  }

  protected void updatePutter() {
    putter =
        ttl != 0 || maxIdleTime != 0
            ? cache::fastPut
            : (key, value, ttl, ttlUnit, maxIdleTime, maxIdleTimeUnit) -> cache.fastPut(key, value);
  }

  @Override
  public void setExpireAfterWrite(Duration expireAfterWrite) {
    ttl = expireAfterWrite.toMillis();
    ttlUnit = TimeUnit.MILLISECONDS;
    updatePutter();
  }

  @Override
  public void setExpireAfterAccess(Duration expireAfterAccess) {
    maxIdleTime = expireAfterAccess.toMillis();
    maxIdleTimeUnit = TimeUnit.MILLISECONDS;
    updatePutter();
  }

  @Override
  public void setMaximumSize(int maximumSize) {
    cache.setMaxSize(maximumSize);
  }

  @Override
  public void put(K key, V value) {
    putter.accept(key, value, ttl, ttlUnit, maxIdleTime, maxIdleTimeUnit);
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

  @FunctionalInterface
  static interface CachePutter<K, V> {

    public void accept(
        K key, V value, long ttl, TimeUnit ttlUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit);
  }
}
