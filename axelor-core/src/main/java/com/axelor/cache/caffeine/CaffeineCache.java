/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.caffeine;

import com.axelor.cache.AxelorCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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

  protected final LoadingCache<K, Lock> locks =
      Caffeine.newBuilder().weakValues().build(k -> new ReentrantLock());

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

  @Override
  public Lock getLock(K key) {
    return locks.get(key);
  }
}
