/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.caffeine;

import com.github.benmanes.caffeine.cache.LoadingCache;
import java.util.Map;
import java.util.Set;

/**
 * Caffeine loading cache
 *
 * <p>This wraps a {@link com.github.benmanes.caffeine.cache.LoadingCache}.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class CaffeineLoadingCache<K, V> extends CaffeineCache<K, V> {

  public CaffeineLoadingCache(LoadingCache<K, V> cache) {
    super(cache);
  }

  @Override
  public V get(K key) {
    return ((LoadingCache<K, V>) cache).get(key);
  }

  @Override
  public Map<K, V> getAll(Set<K> keys) {
    return ((LoadingCache<K, V>) cache).getAll(keys);
  }
}
