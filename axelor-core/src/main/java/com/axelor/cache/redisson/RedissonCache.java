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

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
public class RedissonCache<K, V> implements ConfigurableRedissonCache<K, V> {

  private final RMapCache<K, V> cache;

  private long ttl;

  private TimeUnit ttlUnit;

  private long maxIdleTime;

  private TimeUnit maxIdleTimeUnit;

  private Set<Integer> listenerIds = ConcurrentHashMap.newKeySet();

  public RedissonCache(RMapCache<K, V> cache) {
    this.cache = cache;
  }

  public void setExpireAfterWrite(Duration expireAfterWrite) {
    this.ttl = expireAfterWrite.toMillis();
    this.ttlUnit = TimeUnit.MILLISECONDS;
  }

  public void setExpireAfterAccess(Duration expireAfterAccess) {
    this.maxIdleTime = expireAfterAccess.toMillis();
    this.maxIdleTimeUnit = TimeUnit.MILLISECONDS;
  }

  public void setMaximumSize(int maximumSize) {
    cache.setMaxSize(maximumSize);
  }

  @Override
  public V get(K key) {
    return cache.get(key);
  }

  @Override
  public Map<K, V> getAll(Set<K> keys) {
    return cache.getAll(keys);
  }

  @Override
  public void put(K key, V value) {
    cache.fastPut(key, value, ttl, ttlUnit, maxIdleTime, maxIdleTimeUnit);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    cache.putAll(map);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void invalidate(K key) {
    cache.fastRemove(key);
  }

  @Override
  public void invalidateAll() {
    cache.clear();
  }

  @Override
  public void close() {
    if (!listenerIds.isEmpty()) {
      listenerIds.forEach(cache::removeListener);
      listenerIds.clear();
    }

    cache.destroy();
  }

  @Override
  public long estimatedSize() {
    return cache.size();
  }

  @Override
  public ConcurrentMap<K, V> asMap() {
    return cache;
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
