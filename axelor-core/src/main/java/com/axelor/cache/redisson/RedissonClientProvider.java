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

import com.axelor.cache.redisson.RedissonUtils.Version;
import com.axelor.common.ClassUtils;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.google.inject.Provider;
import jakarta.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redisson client provider
 *
 * <p>This provides a {@link RedissonClient} instance based on a configuration file. For the same
 * configuration file, the same instance is returned.
 */
@Singleton
public class RedissonClientProvider implements Provider<RedissonClient> {

  private final ConcurrentMap<String, RedissonClient> redissonClients = new ConcurrentHashMap<>();

  private static final String DEFAULT_CONFIG_PATH = "redisson.yaml";

  private final RedissonUtils redissonUtils;

  private static final Logger log = LoggerFactory.getLogger(RedissonClientProvider.class);

  @Inject
  public RedissonClientProvider(RedissonUtils redissonUtils) {
    this.redissonUtils = redissonUtils;
  }

  public static RedissonClientProvider getInstance() {
    return Beans.get(RedissonClientProvider.class);
  }

  @Override
  public RedissonClient get() {
    return get(Optional.empty());
  }

  public RedissonClient get(Optional<String> configPath) {
    var path = configPath.orElse(DEFAULT_CONFIG_PATH);
    return redissonClients.computeIfAbsent(path, this::createRedissonClient);
  }

  protected RedissonClient createRedissonClient() {
    return createRedissonClient(DEFAULT_CONFIG_PATH);
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

    // Use AxelorKryo5Codec as default codec instead of Kryo5Codec.
    if (config.getCodec() == null) {
      config.setCodec(new AxelorKryo5Codec());
    }

    var redisson = Redisson.create(config);
    Runtime.getRuntime().addShutdownHook(new Thread(redisson::shutdown));

    log.atInfo()
        .setMessage("Connected to {} on {}")
        .addArgument(
            () -> {
              String name;
              Version version;
              var valkeyVersion = redissonUtils.getValkeyVersion(redisson);

              if (valkeyVersion.isPresent()) {
                name = "Valkey";
                version = valkeyVersion.get();
              } else {
                name = "Redis";
                version = redissonUtils.getRedisVersion(redisson);
              }

              return String.format("%s %s", name, version);
            })
        .addArgument(
            () ->
                redissonUtils.getRedisAddresses(redisson).stream()
                    .map(a -> String.format("%s:%d", a.getHostString(), a.getPort()))
                    .collect(Collectors.joining(", ")))
        .log();

    return redisson;
  }
}
