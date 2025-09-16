/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.redisson;

import java.time.Duration;
import org.apache.commons.lang3.function.TriConsumer;
import org.redisson.api.RMapCacheNative;

/**
 * Redisson cache with native eviction
 *
 * <p>This wraps a {@link org.redisson.api.RMapCache}.
 *
 * <p>Setting TTL requires HPEXPIRE command support introduced in Redis 7.4.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class RedissonCacheNative<K, V> extends AbstractRedissonCache<K, V, RMapCacheNative<K, V>> {

  private Duration ttl;
  private TriConsumer<K, V, Duration> putter;

  public RedissonCacheNative(RMapCacheNative<K, V> cache) {
    super(cache);
    setExpireAfterWrite(null);
  }

  @Override
  public void setExpireAfterWrite(Duration expireAfterWrite) {
    ttl = expireAfterWrite;
    putter =
        ttl != null && Duration.ZERO.compareTo(ttl) != 0
            ? cache::fastPut
            : (key, value, duration) -> cache.fastPut(key, value);
  }

  @Override
  public void setExpireAfterAccess(Duration expireAfterAccess) {
    // RMapCacheNative does not support maxIdleTime.
    // Set ttl instead if not already set.
    if (ttl == null) {
      setExpireAfterWrite(expireAfterAccess);
    }
  }

  @Override
  public void setMaximumSize(int maximumSize) {
    // RMapCacheNative does not support maximum size.
    // Set ttl if not already set, in order to have a least some kind of eviction.
    if (ttl == null && maximumSize > 0) {
      setExpireAfterWrite(Duration.ofHours(1));
    }
  }

  @Override
  public void put(K key, V value) {
    putter.accept(key, value, ttl);
  }
}
