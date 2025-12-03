/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.redisson;

import com.axelor.cache.AxelorCache;
import com.axelor.cache.CacheBuilder;
import com.axelor.cache.CacheLoader;
import java.time.Duration;
import org.redisson.api.RMap;
import org.redisson.api.map.MapLoader;
import org.redisson.api.options.ExMapOptions;

/*
 * Abstract Redisson cache builder
 *
 * <p>Weak references are not supported in Redisson collections. When either {@code weakKeys} or
 * {@code weakValues} are used, TTL is set in order to approximate the behavior.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 * @param <M> the type of Redisson map
 * @param <O> the type of Redisson map options
 */
public abstract class AbstractRedissonCacheBuilder<
        K, V, M extends RMap<K, V>, O extends ExMapOptions<?, K, V>>
    extends CacheBuilder<K, V> {

  protected static final String PREFIX = "axelor-cache:";

  protected AbstractRedissonCacheBuilder(String cacheName) {
    super(cacheName);
  }

  protected AbstractRedissonCacheBuilder(CacheBuilder<K, V> builder) {
    super(builder);
  }

  @Override
  public <K1 extends K, V1 extends V> AxelorCache<K1, V1> buildCache(String name) {
    var cache = newMapCache(name);

    @SuppressWarnings("unchecked")
    var redissonCache = (AxelorCache<K1, V1>) newConfiguredCache(cache);

    return redissonCache;
  }

  @Override
  public <K1 extends K, V1 extends V> AxelorCache<K1, V1> buildCache(
      String name, CacheLoader<? super K1, V1> loader) {
    var cache = newMapCache(name, loader);

    @SuppressWarnings("unchecked")
    var redissonCache = (AxelorCache<K1, V1>) newConfiguredCache(cache);

    return redissonCache;
  }

  private O newPrefixedOptions(String name) {
    return newOptions(PREFIX + name);
  }

  protected abstract O newOptions(String name);

  protected abstract M newMapCache(O options);

  protected abstract AbstractRedissonCache<K, V, M> newRedissonCache(M cache);

  private M newMapCache(String name) {
    return newMapCache(newPrefixedOptions(name));
  }

  private <K1 extends K, V1 extends V> MapLoader<K, V> newMapLoader(
      CacheLoader<? super K1, V1> loader) {
    return new MapLoader<>() {
      @SuppressWarnings("unchecked")
      @Override
      public V load(K key) {
        return loader.load((K1) key);
      }

      @Override
      public Iterable<K> loadAllKeys() {
        throw new UnsupportedOperationException();
      }
    };
  }

  private <K1 extends K, V1 extends V> M newMapCache(
      String name, CacheLoader<? super K1, V1> loader) {
    var options = newPrefixedOptions(name);
    options.loader(newMapLoader(loader));
    return newMapCache(options);
  }

  private AbstractRedissonCache<K, V, M> newConfiguredCache(M cache) {
    var redissonCache = newRedissonCache(cache);
    configureCache(redissonCache);
    return redissonCache;
  }

  protected void configureCache(AbstractRedissonCache<K, V, M> cache) {
    var expireAfterWrite = getExpireAfterWrite();

    // No weak references in Redisson collections
    // Setting TTL most closely approximates the behavior
    if ((isWeakKeys() || isWeakValues()) && expireAfterWrite == null) {
      expireAfterWrite = Duration.ofHours(1);
    }

    if (expireAfterWrite != null) {
      cache.setExpireAfterWrite(expireAfterWrite);
    }

    var expireAfterAccess = getExpireAfterAccess();

    if (expireAfterAccess != null) {
      cache.setExpireAfterAccess(expireAfterAccess);
    }

    var maximumSize = getMaximumSize();

    if (maximumSize > 0) {
      cache.setMaximumSize(maximumSize);
    }
  }
}
