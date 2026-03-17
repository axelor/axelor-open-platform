/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.caffeine;

import com.axelor.cache.AxelorCache;
import com.axelor.cache.CacheBuilder;
import com.axelor.cache.CacheLoader;
import com.axelor.cache.event.RemovalCause;
import com.axelor.cache.event.RemovalListener;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.time.Duration;

/**
 * Caffeine cache builder
 *
 * <p>This builds an {@link AxelorCache} wrapping either a {@link
 * com.github.benmanes.caffeine.cache.Cache} or {@link
 * com.github.benmanes.caffeine.cache.LoadingCache}, because Caffeine uses different interfaces
 * depending on whether the cache is loading.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class CaffeineCacheBuilder<K, V> extends CacheBuilder<K, V> {

  private boolean weakKeys;

  private boolean weakValues;

  public CaffeineCacheBuilder() {
    this((String) null);
  }

  public CaffeineCacheBuilder(String cacheName) {
    super(cacheName);
  }

  @Override
  public <K1 extends K, V1 extends V> AxelorCache<K1, V1> buildCache(String name) {
    var caffeine = newCaffeine();

    @SuppressWarnings("unchecked")
    var cache = (Cache<K1, V1>) caffeine.build();

    return new CaffeineCache<>(cache);
  }

  @Override
  public <K1 extends K, V1 extends V> AxelorCache<K1, V1> buildCache(
      String name, CacheLoader<? super K1, V1> loader) {
    var caffeine = newCaffeine();

    @SuppressWarnings("unchecked")
    var cache = (LoadingCache<K1, V1>) caffeine.build(loader::load);

    return new CaffeineLoadingCache<>(cache, loader);
  }

  protected boolean isWeakKeys() {
    return weakKeys;
  }

  /**
   * Specifies that the cache should use weak references for keys.
   *
   * @return this {@code CaffeineCacheBuilder} instance (for chaining)
   */
  public CaffeineCacheBuilder<K, V> weakKeys() {
    this.weakKeys = true;
    return this;
  }

  protected boolean isWeakValues() {
    return weakValues;
  }

  /**
   * Specifies that the cache should use weak references for values.
   *
   * @return this {@code CaffeineCacheBuilder} instance (for chaining)
   */
  public CaffeineCacheBuilder<K, V> weakValues() {
    this.weakValues = true;
    return this;
  }

  @Override
  public CaffeineCacheBuilder<K, V> maximumSize(int maximumSize) {
    super.maximumSize(maximumSize);
    return this;
  }

  @Override
  public CaffeineCacheBuilder<K, V> expireAfterWrite(Duration expireAfterWrite) {
    super.expireAfterWrite(expireAfterWrite);
    return this;
  }

  @Override
  public CaffeineCacheBuilder<K, V> expireAfterAccess(Duration expireAfterAccess) {
    super.expireAfterAccess(expireAfterAccess);
    return this;
  }

  @Override
  public <K1 extends K, V1 extends V> CaffeineCacheBuilder<K1, V1> removalListener(
      RemovalListener<? super K1, ? super V1> removalListener) {
    super.removalListener(removalListener);

    @SuppressWarnings("unchecked")
    var self = (CaffeineCacheBuilder<K1, V1>) this;

    return self;
  }

  @Override
  public CaffeineCacheBuilder<K, V> nonTenantAware() {
    super.nonTenantAware();
    return this;
  }

  private Caffeine<K, V> newCaffeine() {
    var builder = Caffeine.newBuilder();

    if (getMaximumSize() > 0) {
      builder.maximumSize(getMaximumSize());
    }

    if (getExpireAfterWrite() != null) {
      builder.expireAfterWrite(getExpireAfterWrite());
    }

    if (getExpireAfterAccess() != null) {
      builder.expireAfterAccess(getExpireAfterAccess());
    }

    if (isWeakKeys()) {
      builder.weakKeys();
    }

    if (isWeakValues()) {
      builder.weakValues();
    }

    var removalListener = getRemovalListener();

    if (removalListener != null) {
      builder.<K, V>removalListener(
          (key, value, cause) -> removalListener.onRemoval(key, value, toRemovalCause(cause)));
    }

    @SuppressWarnings("unchecked")
    var caffeine = (Caffeine<K, V>) builder;

    return caffeine;
  }

  protected RemovalCause toRemovalCause(com.github.benmanes.caffeine.cache.RemovalCause cause) {
    return switch (cause) {
      case EXPLICIT -> RemovalCause.REMOVED;
      case REPLACED -> RemovalCause.REPLACED;
      case COLLECTED -> RemovalCause.REMOVED;
      case EXPIRED -> RemovalCause.EXPIRED;
      case SIZE -> RemovalCause.REMOVED;
    };
  }
}
