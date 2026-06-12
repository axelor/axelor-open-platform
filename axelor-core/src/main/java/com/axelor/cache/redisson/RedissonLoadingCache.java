/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.redisson;

import com.axelor.cache.CacheLoader;
import com.google.common.collect.ForwardingConcurrentMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import org.redisson.api.RMapCache;

/**
 * Redisson loading cache with scripted eviction
 *
 * <p>This wraps a {@link org.redisson.api.RMapCache} and computes values using the {@link
 * CacheLoader} on cache misses.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class RedissonLoadingCache<K, V> extends RedissonCache<K, V> {

  protected final ConcurrentMap<K, V> map;
  private final CacheLoader<K, V> loader;

  public RedissonLoadingCache(RMapCache<K, V> cache, CacheLoader<K, V> loader) {
    super(cache);
    this.loader = loader;

    // Map view that uses cache loader.
    this.map =
        new ForwardingConcurrentMap<>() {

          @Override
          protected ConcurrentMap<K, V> delegate() {
            return cache;
          }

          @Override
          @SuppressWarnings("unchecked")
          public V get(Object key) {
            try {
              return RedissonLoadingCache.this.get((K) key);
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
            return RedissonLoadingCache.this.get(key, mappingFunction);
          }
        };
  }

  @Override
  public V get(K key) {
    return cache.computeIfAbsent(key, loader::load);
  }

  @Override
  public Map<K, V> getAll(Set<K> keys) {
    var result = new HashMap<>(cache.getAll(keys));

    for (K key : keys) {
      result.computeIfAbsent(key, this::get);
    }

    return Collections.unmodifiableMap(result);
  }

  @Override
  public V get(K key, Function<? super K, ? extends V> mappingFunction) {
    return cache.computeIfAbsent(
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
