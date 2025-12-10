/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.redisson;

import java.time.Duration;
import java.util.Map;
import org.redisson.api.RMapCacheNative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redisson cache with native eviction
 *
 * <p>This implementation wraps a {@link org.redisson.api.RMapCacheNative} and relies on Redis
 * capabilities for handling entry expiration.
 *
 * <p><b>Important Note on Redis Version:</b><br>
 * Setting Time-To-Live (TTL) requires the {@code HPEXPIRE} command, which was introduced in Redis
 * 7.4. Ensure your Redis server version is compatible.
 *
 * <p><b>Limitations compared to standard {@link org.redisson.api.RMapCache}:</b>
 *
 * <ul>
 *   <li><b>Expire After Access:</b> Not natively supported. This implementation falls back to
 *       {@code expireAfterWrite} (TTL) and logs a warning.
 *   <li><b>Maximum Size:</b> Not natively supported. This implementation falls back to a default
 *       TTL (1 hour) if no other TTL is set, and logs a warning.
 * </ul>
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class RedissonCacheNative<K, V> extends AbstractRedissonCache<K, V, RMapCacheNative<K, V>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(RedissonCacheNative.class);

  private volatile Duration ttl;

  public RedissonCacheNative(RMapCacheNative<K, V> cache) {
    super(cache);
  }

  @Override
  public void setExpireAfterWrite(Duration expireAfterWrite) {
    ttl = expireAfterWrite;
  }

  @Override
  public void setExpireAfterAccess(Duration expireAfterAccess) {
    // RMapCacheNative does not support maxIdleTime.
    // Set ttl instead if not already set.
    if (ttl == null) {
      LOGGER.warn(
          "RMapCacheNative does not support expireAfterAccess, using expireAfterWrite instead");
      setExpireAfterWrite(expireAfterAccess);
    }
  }

  @Override
  public void setMaximumSize(int maximumSize) {
    // RMapCacheNative does not support maximum size.
    // Set ttl if not already set, to have at least some kind of eviction.
    if (ttl == null && maximumSize > 0) {
      LOGGER.warn("RMapCacheNative does not support maximumSize, using 1 hour TTL as fallback");
      setExpireAfterWrite(Duration.ofHours(1));
    }
  }

  @Override
  public void put(K key, V value) {
    if (ttl != null && !ttl.isZero()) {
      cache.fastPut(key, value, ttl);
    } else {
      cache.fastPut(key, value);
    }
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    if (ttl != null && !ttl.isZero()) {
      // Scripted Bulk to ensure TTLL is applied on each entry
      cache.putAll(map, ttl);
    } else {
      // Native Bulk (No expiration)
      cache.putAll(map);
    }
  }
}
