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

import com.axelor.common.StringUtils;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.redisnode.RedisMasterSlave;
import org.redisson.api.redisnode.RedisNode;
import org.redisson.api.redisnode.RedisNode.InfoSection;
import org.redisson.api.redisnode.RedisNodes;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.RedisCommand;

/** Redisson utilities */
public class RedissonUtils {

  private RedissonUtils() {}

  public static List<InetSocketAddress> getRedisAddresses(RedissonClient redisson) {
    var config = redisson.getConfig();

    if (config.isClusterConfig()) {
      return getRedisAddressesCluster(redisson);
    }

    if (config.isSentinelConfig()) {
      return getRedisAddressesSentinelMasterSlave(redisson);
    }

    if (config.isSingleConfig()) {
      return getRedisAddressesSingle(redisson);
    }

    return getRedisAddressesMasterSlave(redisson);
  }

  private static List<InetSocketAddress> getRedisAddressesCluster(RedissonClient redisson) {
    var cluster = redisson.getRedisNodes(RedisNodes.CLUSTER);
    return cluster.getMasters().stream().map(RedisNode::getAddr).toList();
  }

  private static List<InetSocketAddress> getRedisAddressesMasterSlave(RedissonClient redisson) {
    return getRedisAddressesMasterSlave(redisson, RedisNodes.MASTER_SLAVE);
  }

  private static List<InetSocketAddress> getRedisAddressesSentinelMasterSlave(
      RedissonClient redisson) {
    return getRedisAddressesMasterSlave(redisson, RedisNodes.SENTINEL_MASTER_SLAVE);
  }

  private static List<InetSocketAddress> getRedisAddressesMasterSlave(
      RedissonClient redisson, RedisNodes<? extends RedisMasterSlave> masterSlaveNodes) {
    var masterSlave = redisson.getRedisNodes(masterSlaveNodes);
    return List.of(masterSlave.getMaster().getAddr());
  }

  private static List<InetSocketAddress> getRedisAddressesSingle(RedissonClient redisson) {
    var single = redisson.getRedisNodes(RedisNodes.SINGLE);
    return List.of(single.getInstance().getAddr());
  }

  /**
   * Returns the version of the Redis server that the given Redisson client is configured to.
   *
   * <p>If the Redisson client is configured to a cluster, this returns the minimum version of all
   * the Redis servers.
   *
   * <p>This relies on redis_version server info that is present on both Redis and Valkey.
   *
   * @param redisson
   * @return the version of the Redis server
   */
  public static Version getRedisVersion(RedissonClient redisson) {
    return getVersion(redisson, "redis_version").orElse(Version.UNKNOWN);
  }

  /**
   * Returns the version of the Valkey server that the given Redisson client is configured to.
   *
   * <p>If the Redisson client is configured to a cluster, this returns the minimum version of all
   * the Valkey servers.
   *
   * <p>This relies on valkey_version server info that is present on Valkey only.
   *
   * @param redisson
   * @return the optional version of the Valkey server
   */
  public static Optional<Version> getValkeyVersion(RedissonClient redisson) {
    return getVersion(redisson, "valkey_version");
  }

  public static boolean hasCommand(RedissonClient redisson, String command) {
    if (StringUtils.isBlank(command) || !command.matches("\\w+")) {
      return false;
    }

    var executor = ((Redisson) redisson).getCommandExecutor();
    var commandInfo = new RedisCommand<>("COMMAND", "INFO");

    try {
      var result =
          executor
              .readRandomAsync(StringCodec.INSTANCE, commandInfo, command)
              .toCompletableFuture()
              .get();
      if (result instanceof List<?> list) {
        return !list.isEmpty() && list.get(0) != null;
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e.getCause());
    }

    return false;
  }

  public static boolean hasHashFieldExpiration(RedissonClient redisson) {
    return hasCommand(redisson, "HPEXPIRE");
  }

  private static Optional<Version> getVersion(RedissonClient redisson, String versionKey) {
    var config = redisson.getConfig();

    if (config.isClusterConfig()) {
      return getVersionCluster(redisson, versionKey);
    }

    if (config.isSentinelConfig()) {
      return getVersionSentinelMasterSlave(redisson, versionKey);
    }

    if (config.isSingleConfig()) {
      return getVersionSingle(redisson, versionKey);
    }

    return getVersionMasterSlave(redisson, versionKey);
  }

  private static Optional<Version> getVersionCluster(RedissonClient redisson, String versionKey) {
    var cluster = redisson.getRedisNodes(RedisNodes.CLUSTER);
    return findMinVersion(cluster.getMasters().stream(), versionKey);
  }

  private static Optional<Version> getVersionMasterSlave(
      RedissonClient redisson, String versionKey) {
    return getVersionMasterSlave(redisson, RedisNodes.MASTER_SLAVE, versionKey);
  }

  private static Optional<Version> getVersionSentinelMasterSlave(
      RedissonClient redisson, String versionKey) {
    return getVersionMasterSlave(redisson, RedisNodes.SENTINEL_MASTER_SLAVE, versionKey);
  }

  private static Optional<Version> getVersionMasterSlave(
      RedissonClient redisson,
      RedisNodes<? extends RedisMasterSlave> masterSlaveNodes,
      String versionKey) {
    var masterSlave = redisson.getRedisNodes(masterSlaveNodes);
    return getVersion(masterSlave.getMaster(), versionKey);
  }

  private static Optional<Version> getVersionSingle(RedissonClient redisson, String versionKey) {
    var single = redisson.getRedisNodes(RedisNodes.SINGLE);
    return getVersion(single.getInstance(), versionKey);
  }

  private static Optional<Version> getVersion(RedisNode node, String versionKey) {
    var version = node.info(InfoSection.SERVER).get(versionKey);
    return Optional.ofNullable(version).map(Version::parse);
  }

  private static Optional<Version> findMinVersion(
      Stream<? extends RedisNode> nodes, String versionKey) {
    return nodes
        .map(node -> getVersion(node, versionKey))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .reduce(
            (version, otherVersion) ->
                version.compareTo(otherVersion) <= 0 ? version : otherVersion);
  }
}
