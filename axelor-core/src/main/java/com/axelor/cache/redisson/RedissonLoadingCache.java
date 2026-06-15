/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.redisson;

import com.axelor.cache.CacheLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

  private final CacheLoader<K, V> loader;

  public RedissonLoadingCache(RMapCache<K, V> cache, CacheLoader<K, V> loader) {
    super(cache);
    this.loader = loader;
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
    return cache.computeIfAbsent(key, mappingFunction);
  }
}
