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

import com.axelor.app.settings.BeanConfigurator;
import com.axelor.cache.CacheProviderInfo;
import com.axelor.cache.redisson.RedissonUtils.Version;
import com.axelor.common.ClassUtils;
import com.axelor.common.StringUtils;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.google.inject.Provider;
import jakarta.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
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
    return get(Collections.emptyMap(), "");
  }

  public RedissonClient get(CacheProviderInfo info) {
    return get(info.getConfig(), info.getConfigPrefix());
  }

  public RedissonClient get(Map<String, String> config, String prefix) {
    var path = config.isEmpty() ? DEFAULT_CONFIG_PATH : config.get("path");

    if (StringUtils.notBlank(path)) {
      return redissonClients.computeIfAbsent(path, this::createRedissonClient);
    }

    return redissonClients.computeIfAbsent(prefix, k -> createRedissonClient(config, prefix));
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

    return createRedissonClient(config, path);
  }

  protected RedissonClient createRedissonClient(Map<String, String> properties, String prefix) {
    var config = createConfig(properties);
    return createRedissonClient(config, prefix + "*");
  }

  protected RedissonClient createRedissonClient(Config config, String configSource) {
    // Use AxelorKryo5Codec as default codec instead of Kryo5Codec.
    if (config.getCodec() == null) {
      config.setCodec(new AxelorKryo5Codec());
    }

    var redisson = Redisson.create(config);
    Runtime.getRuntime().addShutdownHook(new Thread(redisson::shutdown));

    log.atInfo()
        .setMessage("{} connected to {} on {}")
        .addArgument(
            () -> "Redisson" + (StringUtils.notBlank(configSource) ? ":" + configSource : ""))
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

  protected Config createConfig(Map<String, String> properties) {
    var config = new Config();

    // Group properties into server and non-server configs
    var groupedConfigs =
        properties.entrySet().stream()
            .collect(
                Collectors.partitioningBy(
                    e -> {
                      var key = e.getKey();
                      var parts = key.split("\\.", 2);
                      return parts.length > 1 && parts[0].toLowerCase().endsWith("config");
                    }));

    // Gather server configs
    var serverConfigs =
        groupedConfigs.get(true).stream()
            .collect(
                Collectors.groupingBy(
                    entry -> entry.getKey().split("\\.", 2)[0],
                    Collectors.toMap(entry -> entry.getKey().split("\\.", 2)[1], Entry::getValue)));

    // Handle server configs
    serverConfigs.forEach(
        (type, serverSettings) -> {
          var serverConfig =
              switch (type.toLowerCase()) {
                case String s when s.startsWith("single") -> config.useSingleServer();
                case String s when s.startsWith("master") -> config.useMasterSlaveServers();
                case String s when s.startsWith("cluster") -> config.useClusterServers();
                case String s when s.startsWith("sentinel") -> config.useSentinelServers();
                case String s when s.startsWith("replicated") -> config.useReplicatedServers();
                default -> null;
              };

          if (serverConfig == null) {
            log.error("Unsupported server config: {}", type);
            return;
          }

          // Set properties for the server config
          serverSettings.forEach(
              (key, value) -> BeanConfigurator.setField(serverConfig, key, value));
        });

    // Set properties for the non-server configs
    groupedConfigs
        .get(false)
        .forEach(entry -> BeanConfigurator.setField(config, entry.getKey(), entry.getValue()));

    return config;
  }
}
