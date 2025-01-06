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
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RMapCache;

/**
 * Redisson cache
 *
 * <p>This wraps a {@link org.redisson.api.RMapCache}.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class RedissonCache<K, V> implements AxelorCache<K, V> {

  private final RMapCache<K, V> cache;

  private long ttl;

  private TimeUnit ttlUnit;

  private long maxIdleTime;

  private TimeUnit maxIdleTimeUnit;

  public RedissonCache(RMapCache<K, V> cache) {
    this.cache = cache;
  }

  public void setTTL(Duration duration) {
    this.ttl = duration.toMillis();
    this.ttlUnit = TimeUnit.MILLISECONDS;
  }

  public void setMaxIdleTime(Duration duration) {
    this.maxIdleTime = duration.toMillis();
    this.maxIdleTimeUnit = TimeUnit.MILLISECONDS;
  }

  @Override
  public V get(K key) {
    return cache.get(key);
  }

  @Override
  public void put(K key, V value) {
    cache.fastPut(key, value, ttl, ttlUnit, maxIdleTime, maxIdleTimeUnit);
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
}
