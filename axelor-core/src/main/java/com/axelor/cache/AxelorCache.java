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
package com.axelor.cache;

import jakarta.annotation.Nullable;
import java.io.Closeable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cache interface for wrapping different cache implementations
 *
 * <p>This interface provides fast and simple operations. For full map capabilities, use {@link
 * #asMap()}.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public interface AxelorCache<K, V> extends Iterable<Map.Entry<K, V>>, Closeable {

  /**
   * Returns the value associated with the {@code key} in this cache, obtaining that value from the
   * {@link CacheLoader} if defined and if necessary, otherwise returns {@code null} if there is no
   * cached value for the {@code key}.
   *
   * @param key the key whose associated value is to be returned
   * @return the value to which the specified key is mapped, or {@code null} if this map contains no
   *     mapping for the key
   */
  @Nullable
  V get(K key);

  /**
   * Returns the value associated with the {@code key} in this cache, obtaining that value from the
   * {@code mappingFunction} if necessary.
   *
   * <p>Note that the {@code mappingFunction} is called only if there is no cached value after
   * calling the cache loader if defined.
   *
   * @param key the key with which the specified value is to be associated
   * @param mappingFunction the function to compute a value
   * @return the current (existing or computed) value associated with the specified key, or null if
   *     the computed value is null
   */
  @Nullable
  default V get(K key, Function<? super K, ? extends V> mappingFunction) {
    return asMap().computeIfAbsent(key, mappingFunction);
  }

  /**
   * Returns a map of the values associated with the {@code keys} in this cache, using {@link
   * CacheLoader} if defined and if necessary.
   *
   * @param keys the keys whose associated values are to be returned
   * @return an unmodifiable mapping of keys to values for the specified keys in this cache
   */
  default Map<K, V> getAll(Set<K> keys) {
    return keys.stream().collect(Collectors.toUnmodifiableMap(Function.identity(), this::get));
  }

  /**
   * Associates the {@code value} with the {@code key}.
   *
   * @param key key with which the specified value is to be associated
   * @param value value to be associated with the specified key
   */
  void put(K key, V value);

  /**
   * Copies all of the mappings from the specified map to the cache.
   *
   * <p>The effect of this call is equivalent to that of calling {@code put(key, value)} on this map
   * once for each mapping from {@code key} to {@code value} in the specified map.
   *
   * @param map the mappings to be stored in this cache
   */
  default void putAll(Map<? extends K, ? extends V> map) {
    map.forEach(this::put);
  }

  /**
   * Discards any cached value for the {@code key}.
   *
   * @param key the key whose mapping is to be removed from the cache
   */
  void invalidate(K key);

  /** Discards all cached values. */
  void invalidateAll();

  /**
   * Returns the approximate number of entries in this cache. The actual count may differ because of
   * concurrent updates and pending invalidations.
   *
   * @return the estimated size of the cache
   */
  long estimatedSize();

  /**
   * Returns a view of the entries stored in this cache as a thread-safe map. Modifications made to
   * the map directly affect the cache.
   *
   * <p>Note that if the cache has a cache loader, it will be used. This differs from Caffeine's
   * Cache#asMap() and is designed to match Redisson RMap behavior.
   *
   * @return a thread-safe view of this cache supporting {@link ConcurrentMap} operations
   */
  ConcurrentMap<K, V> asMap();

  /**
   * Returns an iterator over the entries in this cache.
   *
   * @return an iterator over the entries in this cache
   */
  default Iterator<Entry<K, V>> iterator() {
    return asMap().entrySet().iterator();
  }

  /** Signals that the cache is no longer in use and releases any resources. */
  default void close() {
    // Do nothing by default
  }

  /**
   * Performs any pending maintenance operations needed by the cache. Exactly which activities are
   * performed -- if any -- is implementation-dependent.
   */
  default void cleanUp() {
    // Do nothing by default
  }

  /**
   * Returns key-specific lock for this cache.
   *
   * @param key
   * @return reentrant lock
   */
  Lock getLock(K key);
}
