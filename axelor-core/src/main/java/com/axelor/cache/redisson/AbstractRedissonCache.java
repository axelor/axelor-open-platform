/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.redisson;

import com.axelor.cache.AxelorCache;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import org.redisson.api.RMap;

/**
 * Redisson cache with configurability of cache eviction policies.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 * @param <M> the type of Redisson map
 */
public abstract class AbstractRedissonCache<K, V, M extends RMap<K, V>>
    implements AxelorCache<K, V> {

  protected final M cache;

  protected AbstractRedissonCache(M cache) {
    this.cache = cache;
  }

  public abstract void setExpireAfterWrite(Duration expireAfterWrite);

  public abstract void setExpireAfterAccess(Duration expireAfterAccess);

  public abstract void setMaximumSize(int maximumSize);

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
    cache.fastPut(key, value);
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
