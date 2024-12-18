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
    return CacheConfig.getHibernateCacheProvider()
        .map(provider -> RedissonClientProvider.getInstance().get(provider.getConfig()))
        .orElseThrow(() -> new IllegalStateException("Hibernate cache provider not configured"));
  }
}
