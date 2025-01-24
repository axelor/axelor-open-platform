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
package com.axelor.cache.caffeine;

import com.axelor.cache.AxelorCache;
import com.github.benmanes.caffeine.cache.Cache;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Caffeine cache
 *
 * <p>This wraps a {@link com.github.benmanes.caffeine.cache.Cache}.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class CaffeineCache<K, V> implements AxelorCache<K, V> {

  protected final Cache<K, V> cache;

  public CaffeineCache(Cache<K, V> cache) {
    this.cache = cache;
  }

  @Override
  public V get(K key) {
    return cache.getIfPresent(key);
  }

  @Override
  public V get(K key, Function<? super K, ? extends V> mappingFunction) {
    return cache.get(key, mappingFunction);
  }

  @Override
  public Map<K, V> getAll(Set<K> keys) {
    return cache.getAllPresent(keys);
  }

  @Override
  public void put(K key, V value) {
    cache.put(key, value);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    cache.putAll(map);
  }

  @Override
  public void invalidate(K key) {
    cache.invalidate(key);
  }

  @Override
  public void invalidateAll() {
    cache.invalidateAll();
  }

  @Override
  public long estimatedSize() {
    return cache.estimatedSize();
  }

  @Override
  public ConcurrentMap<K, V> asMap() {
    return cache.asMap();
  }

  @Override
  public void close() {
    cache.cleanUp();
  }

  @Override
  public void cleanUp() {
    cache.cleanUp();
  }
}
