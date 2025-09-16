/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.redisson;

import java.util.Map;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.redisson.api.RedissonClient;
import org.redisson.hibernate.RedissonRegionNativeFactory;

public class AxelorRedissonRegionNativeFactory extends RedissonRegionNativeFactory {

  @Override
  protected RedissonClient createRedissonClient(
      StandardServiceRegistry registry, @SuppressWarnings("rawtypes") Map properties) {
    return AxelorRedissonRegionFactory.getRedissonClientFromConfig();
  }
}
