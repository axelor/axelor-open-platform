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

import com.axelor.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ForwardingConcurrentMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Caffeine loading cache
 *
 * <p>This wraps a {@link com.github.benmanes.caffeine.cache.LoadingCache}.
 *
 * <p>In order to match with Redisson behavior, cache loader is always used (normally, Caffeine's
 * get(key, mappingFunction) and asMap() operations skip cache loader).
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class CaffeineLoadingCache<K, V> extends CaffeineCache<K, V> {

  protected final ConcurrentMap<K, V> map;
  protected final CacheLoader<? super K, V> loader;

  public CaffeineLoadingCache(LoadingCache<K, V> cache, CacheLoader<? super K, V> loader) {
    super(cache);
    this.loader = loader;

    // Map view that uses cache loader for consistency with Redisson.
    this.map =
        new ForwardingConcurrentMap<>() {

          @Override
          protected ConcurrentMap<K, V> delegate() {
            return cache.asMap();
          }

          @Override
          @SuppressWarnings("unchecked")
          public V get(Object key) {
            try {
              return CaffeineLoadingCache.this.get((K) key);
            } catch (ClassCastException e) {
              return super.get(key);
            }
          }

          @Override
          public boolean containsKey(Object key) {
            return get(key) != null;
          }

          @Override
          public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
            return CaffeineLoadingCache.this.get(key, mappingFunction);
          }
        };
  }

  @Override
  public V get(K key) {
    return ((LoadingCache<K, V>) cache).get(key);
  }

  /** Try cache loader before mapping function for consistency with Redisson. */
  @Override
  public V get(K key, Function<? super K, ? extends V> mappingFunction) {
    return cache.get(
        key,
        k -> {
          var value = loader.load(k);
          return value != null ? value : mappingFunction.apply(k);
        });
  }

  @Override
  public ConcurrentMap<K, V> asMap() {
    return map;
  }
}
