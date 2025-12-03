/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache;

import com.axelor.db.tenants.TenantResolver;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AxelorCache} implementation that delegates to tenant-specific cache instances.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
class TenantAwareCache<K, V> implements AxelorCache<K, V> {

  private final LoadingCache<String, AxelorCache<K, V>> caches;

  private static final Logger log = LoggerFactory.getLogger(TenantAwareCache.class);

  public TenantAwareCache(Function<String, AxelorCache<K, V>> cacheFactory) {
    this.caches =
        Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofDays(1))
            .removalListener(
                (String tenant, AxelorCache<K, V> cache, RemovalCause cause) -> {
                  if (cache != null) {
                    try {
                      cache.close();
                    } catch (Exception e) {
                      log.error("Failed to close cache for tenant %s".formatted(tenant), e);
                    }
                  }
                })
            .build(cacheFactory::apply);
  }

  private AxelorCache<K, V> getCache() {
    return caches.get(TenantResolver.currentTenantIdentifier());
  }

  /** Closes all underlying tenant-specific caches. */
  @Override
  public void close() {
    // Triggers the removal listener to close all entries.
    caches.invalidateAll();
  }

  // Delegate methods

  @Override
  public V get(K key) {
    return getCache().get(key);
  }

  @Override
  public V get(K key, Function<? super K, ? extends V> mappingFunction) {
    return getCache().get(key, mappingFunction);
  }

  @Override
  public Map<K, V> getAll(Set<K> keys) {
    return getCache().getAll(keys);
  }

  @Override
  public void put(K key, V value) {
    getCache().put(key, value);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    getCache().putAll(map);
  }

  @Override
  public void invalidate(K key) {
    getCache().invalidate(key);
  }

  @Override
  public void invalidateAll() {
    getCache().invalidateAll();
  }

  @Override
  public long estimatedSize() {
    return getCache().estimatedSize();
  }

  @Override
  public ConcurrentMap<K, V> asMap() {
    return getCache().asMap();
  }

  @Override
  public Iterator<Entry<K, V>> iterator() {
    return getCache().iterator();
  }

  @Override
  public void cleanUp() {
    getCache().cleanUp();
  }

  @Override
  public Lock getLock(K key) {
    return getCache().getLock(key);
  }
}
