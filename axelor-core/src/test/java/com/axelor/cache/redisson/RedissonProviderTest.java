/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.redisson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.inject.Beans;
import com.axelor.test.GuiceExtension;
import com.axelor.test.GuiceModules;
import com.google.inject.AbstractModule;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.ReadMode;
import org.redisson.config.TransportMode;

@ExtendWith(GuiceExtension.class)
@GuiceModules({RedissonProviderTest.TestModule.class})
class RedissonProviderTest {

  public static class TestModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(Beans.class).asEagerSingleton();
    }
  }

  @Test
  void testSingleServerConfig() throws MalformedURLException, URISyntaxException {
    var properties =
        Map.ofEntries(
            // Server config
            Map.entry("single-server-config.address", "redis://localhost:6379"),
            Map.entry("single-server-config.database", "0"),
            Map.entry("single-server-config.connection-minimum-idle-size", "5"),
            Map.entry("single-server-config.connection-pool-size", "10"),
            Map.entry("single-server-config.ssl-truststore", "file:truststore"),
            // Non-server config
            Map.entry("threads", "4"),
            Map.entry("netty-threads", "8"),
            Map.entry("codec", JsonJacksonCodec.class.getName()),
            Map.entry("transport-mode", "EPOLL"));

    var config = RedissonProvider.createConfig(properties);
    var singleServer = config.useSingleServer();

    assertEquals("redis://localhost:6379", singleServer.getAddress());
    assertEquals(0, singleServer.getDatabase());
    assertEquals(5, singleServer.getConnectionMinimumIdleSize());
    assertEquals(10, singleServer.getConnectionPoolSize());
    assertEquals(new URI("file:truststore").toURL(), singleServer.getSslTruststore());
    assertEquals(4, config.getThreads());
    assertEquals(8, config.getNettyThreads());
    assertInstanceOf(JsonJacksonCodec.class, config.getCodec());
    assertEquals(TransportMode.EPOLL, config.getTransportMode());
  }

  @Test
  void testMasterSlaveConfig() {
    var properties =
        Map.ofEntries(
            Map.entry("master-slave-servers-config.master-address", "redis://localhost:6379"),
            Map.entry(
                "master-slave-servers-config.slave-addresses",
                "redis://localhost:6380, redis://localhost:6381"),
            Map.entry("master-slave-servers-config.database", "0"),
            Map.entry("master-slave-servers-config.read-mode", "MASTER_SLAVE"));

    var config = RedissonProvider.createConfig(properties);

    var masterSlaveServers = config.useMasterSlaveServers();
    assertEquals("redis://localhost:6379", masterSlaveServers.getMasterAddress());
    var slaveAddresses = masterSlaveServers.getSlaveAddresses();
    assertTrue(slaveAddresses.contains("redis://localhost:6380"));
    assertTrue(slaveAddresses.contains("redis://localhost:6381"));
    assertEquals(0, masterSlaveServers.getDatabase());
    assertEquals(ReadMode.MASTER_SLAVE, masterSlaveServers.getReadMode());
  }

  @Test
  void testClusterConfig() {
    var properties =
        Map.ofEntries(
            Map.entry(
                "cluster-servers-config.node-addresses",
                "redis://localhost:7001, redis://localhost:7002"),
            Map.entry("cluster-servers-config.scan-interval", "2000"));

    var config = RedissonProvider.createConfig(properties);

    var clusterServers = config.useClusterServers();
    var nodeAddresses = clusterServers.getNodeAddresses();
    assertTrue(nodeAddresses.contains("redis://localhost:7001"));
    assertTrue(nodeAddresses.contains("redis://localhost:7002"));
    assertEquals(2000, clusterServers.getScanInterval());
  }

  @Test
  void testSentinelConfig() {
    var properties =
        Map.ofEntries(
            Map.entry("sentinel-servers-config.master-name", "my-master"),
            Map.entry(
                "sentinel-servers-config.sentinel-addresses",
                "redis://localhost:26379,redis://localhost:26380"),
            Map.entry("sentinel-servers-config.database", "0"));

    var config = RedissonProvider.createConfig(properties);

    var sentinelServers = config.useSentinelServers();
    assertEquals("my-master", sentinelServers.getMasterName());
    var sentinelAddresses = sentinelServers.getSentinelAddresses();
    assertTrue(sentinelAddresses.contains("redis://localhost:26379"));
    assertTrue(sentinelAddresses.contains("redis://localhost:26380"));
    assertEquals(0, sentinelServers.getDatabase());
  }

  @Test
  void testReplicatedConfig() {
    var properties =
        Map.ofEntries(
            Map.entry(
                "replicated-servers-config.node-addresses",
                "redis://localhost:8001,redis://localhost:8002"),
            Map.entry("replicated-servers-config.scan-interval", "2000"),
            Map.entry("replicated-servers-config.database", "1"));

    var config = RedissonProvider.createConfig(properties);

    var replicatedServers = config.useReplicatedServers();
    var nodeAddresses = replicatedServers.getNodeAddresses();
    assertTrue(nodeAddresses.contains("redis://localhost:8001"));
    assertTrue(nodeAddresses.contains("redis://localhost:8002"));
    assertEquals(2000, replicatedServers.getScanInterval());
    assertEquals(1, replicatedServers.getDatabase());
  }

  @Test
  void testInvalidConfig() {
    var nonExistingProps = Map.of("non-existing-property", "some-value");
    assertThrows(
        Exception.class,
        () -> RedissonProvider.createConfig(nonExistingProps),
        "Non-existing property");

    var invalidValueProps = Map.of("transport-mode", "NOT_EXISTING");
    assertThrows(
        Exception.class,
        () -> RedissonProvider.createConfig(invalidValueProps),
        "Invalid transport mode value");

    var invalidValueTypeProps = Map.of("single-server-config.database", "MyString");
    assertThrows(
        Exception.class,
        () -> RedissonProvider.createConfig(invalidValueTypeProps),
        "Invalid database value type");

    var incompatibleClassProps = Map.of("codec", Object.class.getName());
    assertThrows(
        Exception.class,
        () -> RedissonProvider.createConfig(incompatibleClassProps),
        "Incompatible codec class");
  }
}
