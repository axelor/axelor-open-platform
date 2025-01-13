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
package com.axelor.cache;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.axelor.cache.caffeine.CaffeineCacheBuilder;
import com.axelor.cache.redisson.RedissonCacheBuilder;
import com.axelor.cache.redisson.RedissonCacheNativeBuilder;
import com.axelor.common.Inflector;
import com.axelor.test.GuiceJunit5Test;
import com.axelor.test.GuiceModules;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@GuiceModules({AbstractBaseCache.CacheTestModule.class})
@TestMethodOrder(MethodOrderer.MethodName.class)
class CacheBuilderTest extends GuiceJunit5Test {

  // Enable this to test redisson-native with expireAfterWrite
  // Currently only supported by Redis 7.4+
  private static final boolean HAS_REDIS_HPEXPIRE = false;

  private static final Duration TTL = Duration.ofMillis(500);

  @BeforeAll
  static void setUp() {
    RedisTest.startRedis();
  }

  @AfterAll
  static void tearDown() {
    RedisTest.stopRedis();
  }

  enum CacheType {
    CAFFEINE(CaffeineCacheBuilder::new),
    REDISSON(RedissonCacheBuilder::new),
    REDISSON_NATIVE(RedissonCacheNativeBuilder::new);

    private final Function<String, CacheBuilder<String, Object>> factory;

    CacheType(Function<String, CacheBuilder<String, Object>> factory) {
      this.factory = factory;
    }

    public Function<String, CacheBuilder<String, Object>> getFactory() {
      return factory;
    }

    @Override
    public String toString() {
      return Inflector.getInstance().dasherize(name());
    }
  }

  @ParameterizedTest(name = "{0} - Basic Cache Operations")
  @EnumSource(CacheType.class)
  void testBasicCacheOperations(CacheType cacheType) {
    doBasicCacheOperations(cacheType.getFactory());
  }

  @ParameterizedTest(name = "{0} - Cache Loader Operations")
  @EnumSource(CacheType.class)
  void testCacheLoaderOperations(CacheType cacheType) {
    doCacheLoaderOperations(cacheType.getFactory());
  }

  // redisson-native with expireAfterWrite requires HPEXPIRE command introduced in Redis 7.4
  @ParameterizedTest(name = "{0} - Expire After Write Operations")
  @EnumSource(value = CacheType.class)
  void testExpireAfterWriteOperations(CacheType cacheType) {
    assumeTrue(
        HAS_REDIS_HPEXPIRE || cacheType != CacheType.REDISSON_NATIVE,
        "redisson-native expireAfterWrite requires Redis HPEXPIRE support");
    doExpireAfterWrite(cacheType.getFactory());
  }

  // redisson-native does not support expireAfterAccess (it behaves like expireAfterWrite)
  @ParameterizedTest(name = "{0} - Expire After Access Operations")
  @EnumSource(
      value = CacheType.class,
      mode = EnumSource.Mode.EXCLUDE,
      names = {"REDISSON_NATIVE"})
  void testExpireAfterAccessOperations(CacheType cacheType) {
    doExpireAfterAccess(cacheType.getFactory());
  }

  @ParameterizedTest(name = "{0} - Map Operations")
  @EnumSource(CacheType.class)
  void testMapOperations(CacheType cacheType) {
    doMapOperations(cacheType.getFactory());
  }

  private void doBasicCacheOperations(
      Function<String, CacheBuilder<String, Object>> cacheBuilderFactory) {
    useCache(
        cacheBuilderFactory.apply("test-cache").build(),
        cache -> {
          assertNotNull(cache, "Cache should be created");
          assertEquals(0, cache.estimatedSize(), "Cache should be empty initially");

          cache.put("key1", "value1");
          assertEquals("value1", cache.get("key1"), "Should return cached value");
          assertNull(cache.get("key2"), "Should return null for non-existent key");
          assertEquals(1, cache.estimatedSize(), "Cache should contain one entry");

          cache.put("key2", "value2");
          assertEquals(2, cache.estimatedSize(), "Cache should contain two entries");

          cache.invalidate("key1");
          assertNull(cache.get("key1"), "Should return null after invalidation");
          assertEquals(
              1, cache.estimatedSize(), "Cache should contain one entry after invalidation");

          cache.invalidateAll();
          assertEquals(0, cache.estimatedSize(), "Cache should be empty after invalidation of all");
        });
  }

  private void doCacheLoaderOperations(
      Function<String, CacheBuilder<String, Object>> cacheBuilderFactory) {
    useCache(
        cacheBuilderFactory.apply("test-cache-loader").build(key -> "loaded-" + key),
        cache -> {
          assertNotNull(cache, "Cache should be created");

          assertEquals("loaded-key1", cache.get("key1"), "Should call loader");

          assertEquals("loaded-key1", cache.get("key1"), "Should not recall loader");

          assertEquals(
              "loaded-key2", cache.get("key2"), "Should return loaded value for different key");

          cache.put("key3", "value3");
          assertEquals("value3", cache.get("key3"), "Should not call loader for put value");

          cache.invalidate("key1");
          assertEquals("loaded-key1", cache.get("key1"), "Should reload for invalidated key");
        });
  }

  private void doExpireAfterWrite(
      Function<String, CacheBuilder<String, Object>> cacheBuilderFactory) {
    useCache(
        cacheBuilderFactory.apply("test-write-expiry").expireAfterWrite(TTL).build(),
        cache -> {
          cache.put("key1", "value1");
          assertEquals("value1", cache.get("key1"), "Should return cached value");

          await()
              .atMost(TTL)
              .pollDelay(TTL.dividedBy(2))
              .untilAsserted(
                  () ->
                      assertEquals(
                          "value1", cache.get("key1"), "Should not expire before write timeout"));

          await()
              .atMost(TTL)
              .pollDelay(TTL.dividedBy(2))
              .untilAsserted(
                  () -> {
                    var result = cache.get("key1");
                    assertNull(result, "Should expire after write timeout");
                  });
        });
  }

  private void doExpireAfterAccess(
      Function<String, CacheBuilder<String, Object>> cacheBuilderFactory) {
    useCache(
        cacheBuilderFactory.apply("test-access-expiry").expireAfterAccess(TTL).build(),
        cache -> {
          cache.put("key1", "value1");
          assertEquals("value1", cache.get("key1"), "Should return cached value");

          await()
              .atMost(TTL)
              .pollDelay(TTL.dividedBy(2))
              .untilAsserted(
                  () ->
                      assertEquals(
                          "value1",
                          cache.get("key1"),
                          "Should not expire before last access timeout"));

          await()
              .atMost(TTL)
              .pollDelay(TTL.dividedBy(2))
              .untilAsserted(
                  () ->
                      assertEquals(
                          "value1",
                          cache.get("key1"),
                          "Last access should have refreshed last access timeout"));

          await()
              .atMost(TTL.multipliedBy(2))
              .pollDelay(TTL)
              .untilAsserted(
                  () -> assertNull(cache.get("key1"), "Should expire after last access timeout"));
        });
  }

  private void doMapOperations(Function<String, CacheBuilder<String, Object>> cacheBuilderFactory) {
    useCache(
        cacheBuilderFactory.apply("test-map").build(),
        cache -> {
          var map = cache.asMap();
          assertNotNull(map, "Cache should return a map view");
          assertEquals(0, map.size(), "Map should be empty initially");

          map.put("key1", "value1");
          assertEquals("value1", map.get("key1"), "Should return cached value");
          assertNull(map.get("key2"), "Should return null for non-existent key");
          assertEquals(1, map.size(), "Map should contain one entry");

          assertEquals(
              "value2",
              map.computeIfAbsent("key2", key -> "value2"),
              "Should return computed value");
          assertEquals(2, map.size(), "Map should contain two entries");

          assertEquals("value1", map.remove("key1"), "Should return removed value");

          assertEquals(
              cache.get("key1"), map.get("key1"), "Cache and map should be in sync - key1");
          assertEquals(
              cache.get("key2"), map.get("key2"), "Cache and map should be in sync - key2");
          assertEquals(cache.estimatedSize(), map.size(), "Cache and map should be in sync - size");
        });
  }

  private void useCache(
      AxelorCache<String, Object> cache, Consumer<AxelorCache<String, Object>> consumer) {
    cache.invalidateAll();
    try {
      consumer.accept(cache);
    } finally {
      cache.invalidateAll();
      cache.close();
    }
  }
}
