/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.apache.shiro.cache.jcache;

import com.axelor.db.tenants.TenantResolver;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JCache implementation that delegates to tenant-specific cache instances.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
class TenantAwareJCache<K, V> implements Cache<K, V> {

  private final LoadingCache<String, Cache<K, V>> caches;

  private static final long MIN_EVICTION_MINUTES = 24 * 60L; // 1 day

  private static final Logger log = LoggerFactory.getLogger(TenantAwareJCache.class);

  public TenantAwareJCache(Function<String, Cache<K, V>> cacheFactory, long sessionTimeoutMinutes) {
    var evictionMinutes = Math.max(sessionTimeoutMinutes, MIN_EVICTION_MINUTES);
    this.caches =
        Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(evictionMinutes))
            .evictionListener(
                (String tenantId, Cache<K, V> innerCache, RemovalCause cause) -> {
                  if (innerCache != null && !innerCache.isClosed()) {
                    try {
                      innerCache.clear();
                      innerCache.close();
                    } catch (Exception e) {
                      log.error("Failed to close Shiro cache for tenant %s".formatted(tenantId), e);
                    }
                  }
                })
            .build(cacheFactory::apply);
  }

  private Cache<K, V> getCache() {
    return caches.get(TenantResolver.currentTenantIdentifier());
  }

  /** Closes all underlying tenant-specific caches. */
  @Override
  public void close() {
    // Triggers the removal listener to close all entries.
    caches.invalidateAll();
  }

  /** Whether all underlying tenant-specific caches are closed. */
  @Override
  public boolean isClosed() {
    return caches.estimatedSize() == 0;
  }

  // Delegate methods

  @Override
  public void clear() {
    getCache().clear();
  }

  @Override
  public boolean containsKey(K key) {
    return getCache().containsKey(key);
  }

  @Override
  public void deregisterCacheEntryListener(
      CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
    getCache().deregisterCacheEntryListener(cacheEntryListenerConfiguration);
  }

  @Override
  public V get(K key) {
    return getCache().get(key);
  }

  @Override
  public Map<K, V> getAll(Set<? extends K> keys) {
    return getCache().getAll(keys);
  }

  @Override
  public V getAndPut(K key, V value) {
    return getCache().getAndPut(key, value);
  }

  @Override
  public V getAndRemove(K key) {
    return getCache().getAndRemove(key);
  }

  @Override
  public V getAndReplace(K key, V value) {
    return getCache().getAndReplace(key, value);
  }

  @Override
  public CacheManager getCacheManager() {
    return getCache().getCacheManager();
  }

  @Override
  public <C extends Configuration<K, V>> C getConfiguration(Class<C> clazz) {
    return getCache().getConfiguration(clazz);
  }

  @Override
  public String getName() {
    return getCache().getName();
  }

  @Override
  public <T> T invoke(K key, EntryProcessor<K, V, T> entryProcessor, Object... arguments)
      throws EntryProcessorException {
    return getCache().invoke(key, entryProcessor, arguments);
  }

  @Override
  public <T> Map<K, EntryProcessorResult<T>> invokeAll(
      Set<? extends K> keys, EntryProcessor<K, V, T> entryProcessor, Object... arguments) {
    return getCache().invokeAll(keys, entryProcessor, arguments);
  }

  @Override
  public Iterator<Entry<K, V>> iterator() {
    return getCache().iterator();
  }

  @Override
  public void loadAll(
      Set<? extends K> keys, boolean replaceExistingValues, CompletionListener completionListener) {
    getCache().loadAll(keys, replaceExistingValues, completionListener);
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
  public boolean putIfAbsent(K key, V value) {
    return getCache().putIfAbsent(key, value);
  }

  @Override
  public void registerCacheEntryListener(
      CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
    getCache().registerCacheEntryListener(cacheEntryListenerConfiguration);
  }

  @Override
  public boolean remove(K key) {
    return getCache().remove(key);
  }

  @Override
  public boolean remove(K key, V oldValue) {
    return getCache().remove(key, oldValue);
  }

  @Override
  public void removeAll() {
    getCache().removeAll();
  }

  @Override
  public void removeAll(Set<? extends K> keys) {
    getCache().removeAll(keys);
  }

  @Override
  public boolean replace(K key, V value) {
    return getCache().replace(key, value);
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    return getCache().replace(key, oldValue, newValue);
  }

  @Override
  public <T> T unwrap(Class<T> clazz) {
    return getCache().unwrap(clazz);
  }
}
