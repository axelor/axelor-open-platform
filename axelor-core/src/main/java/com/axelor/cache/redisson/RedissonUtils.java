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

import com.axelor.inject.Beans;
import jakarta.inject.Singleton;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.redisson.api.RedissonClient;
import org.redisson.api.redisnode.RedisMasterSlave;
import org.redisson.api.redisnode.RedisNode;
import org.redisson.api.redisnode.RedisNode.InfoSection;
import org.redisson.api.redisnode.RedisNodes;

/** Redisson utilities */
@Singleton
public class RedissonUtils {

  public static RedissonUtils getInstance() {
    return Beans.get(RedissonUtils.class);
  }

  public List<InetSocketAddress> getRedisAddresses(RedissonClient redisson) {
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

  private List<InetSocketAddress> getRedisAddressesCluster(RedissonClient redisson) {
    var cluster = redisson.getRedisNodes(RedisNodes.CLUSTER);
    return cluster.getMasters().stream().map(RedisNode::getAddr).toList();
  }

  private List<InetSocketAddress> getRedisAddressesMasterSlave(RedissonClient redisson) {
    return getRedisAddressesMasterSlave(redisson, RedisNodes.MASTER_SLAVE);
  }

  private List<InetSocketAddress> getRedisAddressesSentinelMasterSlave(RedissonClient redisson) {
    return getRedisAddressesMasterSlave(redisson, RedisNodes.SENTINEL_MASTER_SLAVE);
  }

  private List<InetSocketAddress> getRedisAddressesMasterSlave(
      RedissonClient redisson, RedisNodes<? extends RedisMasterSlave> masterSlaveNodes) {
    var masterSlave = redisson.getRedisNodes(masterSlaveNodes);
    return List.of(masterSlave.getMaster().getAddr());
  }

  private List<InetSocketAddress> getRedisAddressesSingle(RedissonClient redisson) {
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
  public Version getRedisVersion(RedissonClient redisson) {
    return getVersion(redisson, "redis_version")
        .orElseThrow(() -> new IllegalStateException("Redis version not found"));
  }

  /**
   * Returns the version of the Valkey server that the given Redisson client is configured to.
   *
   * <p>If the Redisson client is configured to a cluster, this returns the minimum version of all
   * the Valkey servers.
   *
   * <p>This returns an optional version that is empty if server is not Valkey
   *
   * @param redisson
   * @return the optional version of the Valkey server
   */
  public Optional<Version> getValkeyVersion(RedissonClient redisson) {
    return getVersion(redisson, "valkey_version");
  }

  private Optional<Version> getVersion(RedissonClient redisson, String versionKey) {
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

  private Optional<Version> getVersionCluster(RedissonClient redisson, String versionKey) {
    var cluster = redisson.getRedisNodes(RedisNodes.CLUSTER);
    return findMinVersion(cluster.getMasters().stream(), versionKey);
  }

  private Optional<Version> getVersionMasterSlave(RedissonClient redisson, String versionKey) {
    return getVersionMasterSlave(redisson, RedisNodes.MASTER_SLAVE, versionKey);
  }

  private Optional<Version> getVersionSentinelMasterSlave(
      RedissonClient redisson, String versionKey) {
    return getVersionMasterSlave(redisson, RedisNodes.SENTINEL_MASTER_SLAVE, versionKey);
  }

  private Optional<Version> getVersionMasterSlave(
      RedissonClient redisson,
      RedisNodes<? extends RedisMasterSlave> masterSlaveNodes,
      String versionKey) {
    var masterSlave = redisson.getRedisNodes(masterSlaveNodes);
    return getVersion(masterSlave.getMaster(), versionKey);
  }

  private Optional<Version> getVersionSingle(RedissonClient redisson, String versionKey) {
    var single = redisson.getRedisNodes(RedisNodes.SINGLE);
    return getVersion(single.getInstance(), versionKey);
  }

  private Optional<Version> getVersion(RedisNode node, String versionKey) {
    var version = node.info(InfoSection.SERVER).get(versionKey);
    return Optional.ofNullable(version).map(Version::parse);
  }

  private Optional<Version> findMinVersion(Stream<? extends RedisNode> nodes, String versionKey) {
    return nodes
        .map(node -> getVersion(node, versionKey))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .reduce(
            (version, otherVersion) ->
                version.compareTo(otherVersion) <= 0 ? version : otherVersion);
  }

  public static record Version(int major, int minor, int patch) {

    public static Version parse(String version) {
      var parts = version.split("\\.");
      return new Version(
          Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    public int compareTo(Version other) {
      if (major != other.major) {
        return major - other.major;
      }
      if (minor != other.minor) {
        return minor - other.minor;
      }
      return patch - other.patch;
    }

    public String toString() {
      return major + "." + minor + "." + patch;
    }
  }
}
