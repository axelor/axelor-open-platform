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
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
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
  protected static final String PREFIX = "axelor:";

  protected AbstractRedissonCacheBuilder(String cacheName) {
    super(PREFIX + cacheName);
  }

  @Override
  public <K1 extends K, V1 extends V> AxelorCache<K1, V1> build() {
    var cache = newMapCache();

    @SuppressWarnings("unchecked")
    var redissonCache = (AxelorCache<K1, V1>) newConfiguredCache(cache);

    return redissonCache;
  }

  @Override
  public <K1 extends K, V1 extends V> AxelorCache<K1, V1> build(Function<? super K1, V1> loader) {
    var cache = newMapCache(loader);

    @SuppressWarnings("unchecked")
    var redissonCache = (AxelorCache<K1, V1>) newConfiguredCache(cache);

    return redissonCache;
  }

  protected abstract O newOptions();

  protected abstract M newMapCache(O options);

  protected abstract ConfigurableRedissonCache<K, V> newRedissonCache(M cache);

  protected RedissonClient getRedissonClient() {
    return RedissonClientProvider.getInstance().get(getCacheProviderInfo().getConfig());
  }

  private <K1 extends K, V1 extends V> MapLoader<K, V> newMapLoader(
      Function<? super K1, V1> loader) {
    return new MapLoader<>() {
      @SuppressWarnings("unchecked")
      @Override
      public V load(K key) {
        return loader.apply((K1) key);
      }

      @Override
      public Iterable<K> loadAllKeys() {
        throw new UnsupportedOperationException();
      }
    };
  }

  private M newMapCache() {
    return newMapCache(newOptions());
  }

  private <K1 extends K, V1 extends V> M newMapCache(Function<? super K1, V1> loader) {
    var options = newOptions();
    options.loader(newMapLoader(loader));
    return newMapCache(options);
  }

  private ConfigurableRedissonCache<K, V> newConfiguredCache(M cache) {
    var redissonCache = newRedissonCache(cache);
    configureCache(redissonCache);
    return redissonCache;
  }

  private void configureCache(ConfigurableRedissonCache<K, V> cache) {
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
