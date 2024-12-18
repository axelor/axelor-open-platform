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

import com.axelor.common.ClassUtils;
import com.axelor.inject.Beans;
import com.google.inject.Provider;
import jakarta.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

@Singleton
public class RedissonClientProvider implements Provider<RedissonClient> {

  private final ConcurrentMap<String, RedissonClient> redissonClients = new ConcurrentHashMap<>();

  private static final String DEFAULT_CONFIG = "redisson.yaml";

  public static RedissonClientProvider getInstance() {
    return Beans.get(RedissonClientProvider.class);
  }

  @Override
  public RedissonClient get() {
    return get(Optional.empty());
  }

  public RedissonClient get(Optional<String> config) {
    var path = config.orElse(DEFAULT_CONFIG);
    return redissonClients.computeIfAbsent(path, this::createRedissonClient);
  }

  protected RedissonClient createRedissonClient() {
    return createRedissonClient(DEFAULT_CONFIG);
  }

  protected RedissonClient createRedissonClient(String path) {
    Config config;

    try {
      // Try filesystem, then classpath.
      var file = new File(path);
      if (file.exists()) {
        config = Config.fromYAML(file);
      } else {
        var url = ClassUtils.getResource(path);
        if (url != null) {
          config = Config.fromYAML(url);
        } else {
          throw new IllegalArgumentException("Redisson config not found: " + path);
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    if (config.getCodec() == null) {
      // Codec compatible with Java object serialization
      // Default Kryo5Codec doesn't seem to be able to serialize SimpleSession#id
      config.setCodec(new org.redisson.codec.SerializationCodec());
    }

    var redisson = Redisson.create(config);
    Runtime.getRuntime().addShutdownHook(new Thread(redisson::shutdown));

    return redisson;
  }
}
