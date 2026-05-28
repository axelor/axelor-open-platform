/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache;

import com.axelor.cache.caffeine.CaffeineCacheBuilder;
import com.axelor.cache.event.RemovalListener;
import com.axelor.db.tenants.TenantModule;
import java.time.Duration;
import java.util.function.Function;

/**
 * A builder of {@link AxelorCache} instances
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public abstract class CacheBuilder<K, V, B extends CacheBuilder<K, V, B>> {

  private final String cacheName;

  private int maximumSize;

  private Duration expireAfterWrite;

  private Duration expireAfterAccess;

  private boolean tenantAware = true;

  private RemovalListener<? super K, ? super V> removalListener;

  private static final CacheProviderInfo cacheProviderInfo =
      CacheConfig.getAppCacheProvider().orElseGet(() -> new CacheProviderInfo("caffeine"));

  private static final CacheType cacheType =
      cacheProviderInfo
          .getCacheType()
          .orElseThrow(
              () ->
                  new IllegalArgumentException(
                      "Unsupported cache provider: " + cacheProviderInfo.getProvider()));

  private static final StackWalker stackWalker =
      StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

  public static CacheType getCacheType() {
    return cacheType;
  }

  protected CacheBuilder(String cacheName) {
    this.cacheName = cacheName;
  }

  protected CacheBuilder(CacheBuilder<K, V, ?> builder) {
    this(builder.cacheName);
    this.maximumSize = builder.maximumSize;
    this.expireAfterWrite = builder.expireAfterWrite;
    this.expireAfterAccess = builder.expireAfterAccess;
    this.tenantAware = builder.tenantAware;
    this.removalListener = builder.removalListener;
  }

  /**
   * Constructs a new {@code CacheBuilder} instance.
   *
   * <p>The caller class is used together with the specified name as cache name.
   *
   * <p>The cache name is used to create a globally unique cache name, depending on the cache
   * provider.
   *
   * @param name the name used as suffix of the cache name
   * @param <K> the key type of the cache
   * @param <V> the value type of the cache
   * @return a new {@code CacheBuilder} instance
   */
  public static <K, V, B extends CacheBuilder<K, V, B>> B newBuilder(String name) {
    return fromCacheName(stackWalker.getCallerClass().getName() + ":" + name);
  }

  /**
   * Constructs a new {@code CaffeineCacheBuilder} instance for an in-memory cache.
   *
   * @param <K> the key type of the cache
   * @param <V> the value type of the cache
   * @return a new {@code CaffeineCacheBuilder} instance
   */
  public static <K, V> CaffeineCacheBuilder<K, V> newInMemoryBuilder() {
    return new CaffeineCacheBuilder<>();
  }

  /**
   * Constructs a new {@code CacheBuilder} instance for the specified cache name.
   *
   * <p>The cache name is used to create a globally unique cache name, depending on the cache
   * provider.
   *
   * @param name the name of the cache
   * @param <K> the key type of the cache
   * @param <V> the value type of the cache
   * @return a new {@code CacheBuilder} instance
   */
  @SuppressWarnings("unchecked")
  protected static <K, V, B extends CacheBuilder<K, V, B>> B fromCacheName(String name) {
    return (B) cacheType.getCacheBuilder(name);
  }

  /**
   * Returns information about the current cache provider configuration.
   *
   * @return the cache provider information
   */
  public static CacheProviderInfo getCacheProviderInfo() {
    return cacheProviderInfo;
  }

  protected String getCacheName() {
    return cacheName;
  }

  @SuppressWarnings("unchecked")
  protected B self() {
    return (B) this;
  }

  protected int getMaximumSize() {
    return maximumSize;
  }

  /**
   * Sets the maximum number of entries the cache may contain.
   *
   * <p>Depending on the cache provider, this may not be supported and approximation techniques may
   * be used.
   *
   * @param maximumSize the maximum size of the cache
   * @return this {@code CacheBuilder} instance (for chaining)
   */
  public B maximumSize(int maximumSize) {
    this.maximumSize = maximumSize;
    return self();
  }

  protected Duration getExpireAfterWrite() {
    return expireAfterWrite;
  }

  /**
   * Sets the duration after which cache entries will expire following the last write.
   *
   * @param expireAfterWrite the duration after which entries will expire following the last write
   * @return this {@code CacheBuilder} instance (for chaining)
   */
  public B expireAfterWrite(Duration expireAfterWrite) {
    this.expireAfterWrite = expireAfterWrite;
    return self();
  }

  protected Duration getExpireAfterAccess() {
    return expireAfterAccess;
  }

  /**
   * Sets the duration after which cache entries will expire following the last access.
   *
   * @param expireAfterAccess the duration after which entries will expire following the last access
   * @return this {@code CacheBuilder} instance (for chaining)
   */
  public B expireAfterAccess(Duration expireAfterAccess) {
    this.expireAfterAccess = expireAfterAccess;
    return self();
  }

  protected boolean isTenantAware() {
    return tenantAware;
  }

  /**
   * Disables data isolation per tenant. The cache will be shared globally across all tenants when
   * multi-tenancy is active.
   *
   * <p>By default, when multi-tenancy is active, the cache automatically segregates entries based
   * on the current tenant context.
   *
   * @return this {@code CacheBuilder} instance (for chaining)
   */
  public B nonTenantAware() {
    this.tenantAware = false;
    return self();
  }

  @SuppressWarnings("unchecked")
  protected <K1 extends K, V1 extends V> RemovalListener<K1, V1> getRemovalListener() {
    return (RemovalListener<K1, V1>) removalListener;
  }

  /**
   * Specifies a listener instance that caches should notify each time an entry is removed for any
   * {@link com.axelor.cache.event.RemovalCause reason}.
   *
   * @param removalListener the listener instance
   * @return this {@code CacheBuilder} instance (for chaining)
   */
  public <K1 extends K, V1 extends V, B1 extends CacheBuilder<K1, V1, B1>>
      CacheBuilder<K1, V1, B1> removalListener(
          RemovalListener<? super K1, ? super V1> removalListener) {

    @SuppressWarnings("unchecked")
    var self = (CacheBuilder<K1, V1, B1>) this;
    self.removalListener = removalListener;

    return self;
  }

  /**
   * Builds an {@code AxelorCache} which does not automatically load values when keys are requested.
   *
   * @param <K1> the key type of the cache
   * @param <V1> the value type of the cache
   * @return a new {@code AxelorCache} instance having the specified configuration
   */
  public final <K1 extends K, V1 extends V> AxelorCache<K1, V1> build() {
    if (isTenantAware() && TenantModule.isEnabled()) {
      return createTenantAwareCache(
          tenant -> buildCache("%s:%s".formatted(tenant, getCacheName())));
    } else {
      return buildCache(getCacheName());
    }
  }

  protected abstract <K1 extends K, V1 extends V> AxelorCache<K1, V1> buildCache(String name);

  /**
   * Builds an {@code AxelorCache} which either returns an already-loaded value for a given key or
   * atomically computes or retrieves it using the supplied {@code CacheLoader}.
   *
   * @param loader the {@code CacheLoader} used to obtain new values
   * @param <K1> the key type of the cache
   * @param <V1> the value type of the cache
   * @return a new {@code AxelorCache} instance having the specified configuration and using the
   *     specified loader
   */
  public final <K1 extends K, V1 extends V> AxelorCache<K1, V1> build(
      CacheLoader<? super K1, V1> loader) {
    if (isTenantAware() && TenantModule.isEnabled()) {
      return createTenantAwareCache(
          tenant -> buildCache("%s:%s".formatted(tenant, getCacheName()), loader));
    } else {
      return buildCache(getCacheName(), loader);
    }
  }

  protected <K1 extends K, V1 extends V> TenantAwareCache<K1, V1> createTenantAwareCache(
      Function<String, AxelorCache<K1, V1>> cacheFactory) {
    return new TenantAwareCache<>(cacheFactory);
  }

  protected abstract <K1 extends K, V1 extends V> AxelorCache<K1, V1> buildCache(
      String name, CacheLoader<? super K1, V1> loader);
}
