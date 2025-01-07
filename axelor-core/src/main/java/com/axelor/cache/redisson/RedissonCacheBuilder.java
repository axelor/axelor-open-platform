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
package com.axelor.cache.redisson;

import com.axelor.cache.AxelorCache;
import com.axelor.cache.CacheBuilder;
import java.time.Duration;
import java.util.function.Function;
import org.redisson.api.RMapCache;
import org.redisson.api.map.MapLoader;
import org.redisson.api.options.MapCacheOptions;

/**
 * Redisson cache builder
 *
 * <p>This builds an {@link AxelorCache} wrapping a {@link org.redisson.api.RMapCache}.
 *
 * <p>Weak references are not supported in Redisson collections. When either {@code weakKeys} or
 * {@code weakValues} are used, TTL is set in order to approximate the behavior.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class RedissonCacheBuilder<K, V> extends CacheBuilder<K, V> {

  // Prefix for Redis keys
  // https://docs.gitlab.com/ee/development/redis.html#key-naming
  private static final String PREFIX = "axelor:";

  public RedissonCacheBuilder(String cacheName) {
    super(PREFIX + cacheName);
  }

  @Override
  public <K1 extends K, V1 extends V> AxelorCache<K1, V1> build() {
    @SuppressWarnings("unchecked")
    var cache = (RMapCache<K1, V1>) newMapCache();

    return newRedissonCache(cache);
  }

  @Override
  public <K1 extends K, V1 extends V> AxelorCache<K1, V1> build(Function<K, V> loader) {
    @SuppressWarnings("unchecked")
    var cache = (RMapCache<K1, V1>) newMapCache(loader);

    return newRedissonCache(cache);
  }

  private MapCacheOptions<K, V> newOptions() {
    return MapCacheOptions.<K, V>name(getCacheName());
  }

  private RMapCache<K, V> newMapCache() {
    return newMapCache(newOptions());
  }

  private RMapCache<K, V> newMapCache(Function<K, V> loader) {
    var options = newOptions();
    options.loader(
        new MapLoader<K, V>() {
          @Override
          public V load(K key) {
            return loader.apply(key);
          }

          @Override
          public Iterable<K> loadAllKeys() {
            throw new UnsupportedOperationException();
          }
        });
    return newMapCache(options);
  }

  private RMapCache<K, V> newMapCache(MapCacheOptions<K, V> options) {
    var redisson = RedissonClientProvider.getInstance().get(getCacheProviderInfo().getConfig());
    var cache = redisson.getMapCache(options);

    if (getMaximumSize() > 0) {
      cache.setMaxSize(getMaximumSize());
    }

    return cache;
  }

  private <K1 extends K, V1 extends V> RedissonCache<K1, V1> newRedissonCache(
      RMapCache<K1, V1> cache) {
    var redissonCache = new RedissonCache<>(cache);

    var expireAfterWrite = getExpireAfterWrite();

    // No weak references in Redisson collections
    // Setting TTL most closely approximates the behavior
    if ((isWeakKeys() || isWeakValues()) && expireAfterWrite == null) {
      expireAfterWrite = Duration.ofHours(1);
    }

    if (expireAfterWrite != null) {
      redissonCache.setTTL(expireAfterWrite);
    }

    var expireAfterAccess = getExpireAfterAccess();

    if (expireAfterAccess != null) {
      redissonCache.setMaxIdleTime(expireAfterAccess);
    }

    return redissonCache;
  }
}
