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
