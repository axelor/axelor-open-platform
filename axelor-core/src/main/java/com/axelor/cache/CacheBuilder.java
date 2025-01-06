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

import com.axelor.cache.caffeine.CaffeineCacheBuilder;
import com.axelor.cache.redisson.RedissonCacheBuilder;
import java.time.Duration;
import java.util.function.Function;

/**
 * A builder of {@link AxelorCache} instances
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public abstract class CacheBuilder<K, V> {

  private final String cacheName;

  private int maximumSize;

  private Duration expireAfterWrite;

  private Duration expireAfterAccess;

  private boolean weakKeys;

  private boolean weakValues;

  private static final CacheProvider cacheProvider;

  private static final Function<String, CacheBuilder<?, ?>> cacheBuilderFactory;

  static {
    cacheProvider =
        CacheConfig.getAppCacheProvider().orElseGet(() -> new CacheProvider("caffeine"));
    var providerName = cacheProvider.getProvider();

    switch (providerName) {
      case "caffeine":
        cacheBuilderFactory = name -> new CaffeineCacheBuilder<>(name);
        break;
      case "redisson":
        cacheBuilderFactory = name -> new RedissonCacheBuilder<>(name);
        break;
      default:
        throw new IllegalArgumentException("Unsupported cache provider: " + providerName);
    }
  }

  protected CacheBuilder(String cacheName) {
    this.cacheName = cacheName;
  }

  /**
   * Constructs a new {@code CacheBuilder} instance for the specified class.
   *
   * <p>The class is used to create a globally unique cache name, depending on the cache provider.
   * This is suitable if your class contains only one cache. Otherwise, use {@link
   * #newBuilder(Class, String)}.
   *
   * @param cls the class to create the cache for
   * @param <K> the key type of the cache
   * @param <V> the value type of the cache
   * @return a new {@code CacheBuilder} instance
   */
  public static <K, V> CacheBuilder<K, V> newBuilder(Class<?> cls) {
    return newBuilder(cls.getName());
  }

  /**
   * Constructs a new {@code CacheBuilder} instance for the specified class and cache name.
   *
   * <p>The class and the cache name may be used to create a globally unique cache name, depending
   * on the cache provider.
   *
   * @param cls the class to create the cache for
   * @param name the name of the cache
   * @param <K> the key type of the cache
   * @param <V> the value type of the cache
   * @return a new {@code CacheBuilder} instance
   */
  public static <K, V> CacheBuilder<K, V> newBuilder(Class<?> cls, String name) {
    return newBuilder(cls.getName() + ":" + name);
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
  public static <K, V> CacheBuilder<K, V> newBuilder(String name) {
    @SuppressWarnings("unchecked")
    var builder = (CacheBuilder<K, V>) cacheBuilderFactory.apply(name);

    return builder;
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
  public CacheBuilder<K, V> maximumSize(int maximumSize) {
    this.maximumSize = maximumSize;
    return this;
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
  public CacheBuilder<K, V> expireAfterWrite(Duration expireAfterWrite) {
    this.expireAfterWrite = expireAfterWrite;
    return this;
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
  public CacheBuilder<K, V> expireAfterAccess(Duration expireAfterAccess) {
    this.expireAfterAccess = expireAfterAccess;
    return this;
  }

  protected boolean isWeakKeys() {
    return weakKeys;
  }

  /**
   * Specifies that the cache should use weak references for keys.
   *
   * <p>Depending on the cache provider, this may not be supported and approximation techniques may
   * be used.
   *
   * @return this {@code CacheBuilder} instance (for chaining)
   */
  public CacheBuilder<K, V> weakKeys() {
    this.weakKeys = true;
    return this;
  }

  protected boolean isWeakValues() {
    return weakValues;
  }

  /**
   * Specifies that the cache should use weak references for values.
   *
   * <p>Depending on the cache provider, this may not be supported and approximation techniques may
   * be used.
   *
   * @return this {@code CacheBuilder} instance (for chaining)
   */
  public CacheBuilder<K, V> weakValues() {
    this.weakValues = true;
    return this;
  }

  /**
   * Builds a {@code AxelorCache} with the specified configuration.
   *
   * @param <K1> the key type of the cache
   * @param <V1> the value type of the cache
   * @return a new {@code AxelorCache} instance having the specified configuration
   */
  public abstract <K1 extends K, V1 extends V> AxelorCache<K1, V1> build();

  /**
   * Builds a {@code AxelorCache} with the specified configuration and loading function.
   *
   * @param loader the function to compute values when they are not present in the cache
   * @param <K1> the key type of the cache
   * @param <V1> the value type of the cache
   * @return a new {@code AxelorCache} instance having the specified configuration and using the
   *     specified loader
   */
  public abstract <K1 extends K, V1 extends V> AxelorCache<K1, V1> build(
      Function<? super K1, V1> loader);
}
