/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache;

import java.time.Duration;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A distributed variant of {@link TenantAwareCache} that uses TTL-based expiry instead of closing
 * the cache on eviction.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class TenantAwareDistributedCache<K, V> extends TenantAwareCache<K, V> {

  private static final Logger log = LoggerFactory.getLogger(TenantAwareDistributedCache.class);

  public TenantAwareDistributedCache(Function<String, AxelorCache<K, V>> cacheFactory) {
    super(
        tenantId -> {
          var cache = cacheFactory.apply(tenantId);
          ((ExpirableAxelorCache<K, V>) cache).clearExpire();
          return cache;
        },
        (tenantId, innerCache, cause) -> {
          if (innerCache != null) {
            try {
              ((ExpirableAxelorCache<K, V>) innerCache)
                  .expire(Duration.ofMinutes(MIN_EVICTION_MINUTES));
            } catch (Exception e) {
              log.error("Failed to expire cache for tenant %s".formatted(tenantId), e);
            }
          }
        });
  }
}
