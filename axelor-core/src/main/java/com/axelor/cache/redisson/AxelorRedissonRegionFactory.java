/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.redisson;

import com.axelor.cache.CacheConfig;
import java.util.Map;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.redisson.api.RedissonClient;
import org.redisson.hibernate.RedissonRegionFactory;

/**
 * Hibernate Cache region factory based on Redisson with ability to reuse Redisson client from
 * configuration
 */
public class AxelorRedissonRegionFactory extends RedissonRegionFactory {

  @Override
  protected RedissonClient createRedissonClient(
      StandardServiceRegistry registry, @SuppressWarnings("rawtypes") Map properties) {
    return getRedissonClientFromConfig();
  }

  static RedissonClient getRedissonClientFromConfig() {
    return CacheConfig.getHibernateCacheProvider()
        .map(RedissonProvider::get)
        .orElseThrow(() -> new IllegalStateException("Hibernate cache provider not configured"));
  }
}
