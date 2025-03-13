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
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import org.apache.commons.lang3.function.TriConsumer;
import org.redisson.api.RMapCacheNative;

/**
 * Redisson cache with native eviction
 *
 * <p>This wraps a {@link org.redisson.api.RMapCache}.
 *
 * <p>Setting TTL requires HPEXPIRE command support introduced in Redis 7.4.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class RedissonCacheNative<K, V> implements ConfigurableRedissonCache<K, V> {

  private final RMapCacheNative<K, V> cache;
  private Duration ttl;
  private TriConsumer<K, V, Duration> fastPut;

  public RedissonCacheNative(RMapCacheNative<K, V> cache) {
    this.cache = cache;
    setExpireAfterWrite(null);
  }

  public void setExpireAfterWrite(Duration expireAfterWrite) {
    this.ttl = expireAfterWrite;

    if (ttl != null) {
      fastPut = cache::fastPut;
    } else {
      fastPut = (key, value, duration) -> cache.fastPut(key, value);
    }
  }

  public void setExpireAfterAccess(Duration expireAfterAccess) {
    // RMapCacheNative does not support maxIdleTime
    // Set ttl instead if not already set
    if (ttl == null) {
      setExpireAfterWrite(expireAfterAccess);
    }
  }

  public void setMaximumSize(int maximumSize) {
    // RMapCacheNative does not support maximum size
    // Set ttl if not already set, in order to have a least some kind of eviction
    if (ttl == null && maximumSize > 0) {
      setExpireAfterWrite(Duration.ofHours(1));
    }
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
    fastPut.accept(key, value, ttl);
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

  @Override
  public Lock getLock(K key) {
    return cache.getLock(key);
  }
}
