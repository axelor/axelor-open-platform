/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.axelor.cache.event.RemovalCause;
import com.axelor.cache.redisson.RedissonProvider;
import com.axelor.cache.redisson.RedissonUtils;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CacheBuilderTest {

  private static final Duration TTL = Duration.ofMillis(500);

  private static boolean hasHPExpire;

  private static final Logger log = LoggerFactory.getLogger(CacheBuilderTest.class);

  @BeforeAll
  static void setUp() {
    RedisTest.startRedis();
    init();
  }

  @AfterAll
  static void tearDown() {
    RedisTest.stopRedis();
  }

  private static void init() {
    // Check for HPEXPIRE command (Redis 7.4+ and Valkey 9.0+)
    var redisson = RedissonProvider.get();

    hasHPExpire = RedissonUtils.hasHashFieldExpiration(redisson);

    if (!hasHPExpire) {
      log.warn(
          "{} tests that require HPEXPIRE support will be skipped.", CacheType.REDISSON_NATIVE);
    }
  }

  @ParameterizedTest(name = "{0} - Basic Cache Operations")
  @EnumSource(CacheType.class)
  void testBasicCacheOperations(CacheType cacheType) {
    doBasicCacheOperations(cacheType::getCacheBuilder);
  }

  @ParameterizedTest(name = "{0} - Cache Loader Operations")
  @EnumSource(CacheType.class)
  void testCacheLoaderOperations(CacheType cacheType) {
    doCacheLoaderOperations(cacheType::getCacheBuilder);
  }

  // redisson-native with expireAfterWrite requires HPEXPIRE command introduced in Redis 7.4
  @ParameterizedTest(name = "{0} - Expire After Write Operations")
  @EnumSource(value = CacheType.class)
  void testExpireAfterWriteOperations(CacheType cacheType) {
    assumeTrue(
        hasHPExpire || cacheType != CacheType.REDISSON_NATIVE,
        "redisson-native expireAfterWrite requires Redis HPEXPIRE support");
    doExpireAfterWrite(cacheType::getCacheBuilder);
  }

  // redisson-native does not support expireAfterAccess (it behaves like expireAfterWrite)
  @ParameterizedTest(name = "{0} - Expire After Access Operations")
  @EnumSource(
      value = CacheType.class,
      mode = EnumSource.Mode.EXCLUDE,
      names = {"REDISSON_NATIVE"})
  void testExpireAfterAccessOperations(CacheType cacheType) {
    doExpireAfterAccess(cacheType::getCacheBuilder);
  }

  @ParameterizedTest(name = "{0} - Map Operations")
  @EnumSource(CacheType.class)
  void testMapOperations(CacheType cacheType) {
    doMapOperations(cacheType::getCacheBuilder);
  }

  @ParameterizedTest(name = "{0} - Removal Listener Operations")
  @EnumSource(value = CacheType.class)
  void testRemovalListenerOperations(CacheType cacheType) {
    doRemovalListenerOperations(cacheType::getCacheBuilder);
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

          assertEquals(
              "computed-key1",
              cache.get("key1", k -> "computed-" + k),
              "Should call mapping function for non-cached value");

          assertEquals(
              "value2",
              cache.get("key2", k -> "another-computed-" + k),
              "Should not call mapping function for cached value");

          cache.invalidateAll();
          assertEquals(0, cache.estimatedSize(), "Cache should be empty after invalidation of all");
        });
  }

  private void doCacheLoaderOperations(
      Function<String, CacheBuilder<String, Object>> cacheBuilderFactory) {
    useCache(
        cacheBuilderFactory
            .apply("test-cache-loader")
            .build(key -> key.endsWith("-null") ? null : "loaded-" + key),
        cache -> {
          assertNotNull(cache, "Cache should be created");

          assertEquals("loaded-key", cache.get("key"), "Should call loader");

          assertEquals("loaded-key", cache.get("key"), "Should not recall loader");

          assertEquals(
              "loaded-another-key",
              cache.get("another-key"),
              "Should return loaded value for another key");

          cache.put("existing-key", "existing-value");
          assertEquals(
              "existing-value", cache.get("existing-key"), "Should not call loader for put value");

          cache.invalidate("key");
          assertEquals("loaded-key", cache.get("key"), "Should reload for invalidated key");

          assertEquals(
              "loaded-key-function",
              cache.get("key-function", k -> "computed-" + k),
              "Should not call mapping function if loader returns value");

          assertEquals(
              "computed-key-function-null",
              cache.get("key-function-null", k -> "computed-" + k),
              "Should call mapping function if loader returns null");

          var map = cache.asMap();

          assertTrue(map.containsKey("contains-key-map"), "Should call loader returning value");
          assertFalse(
              map.containsKey("contains-key-map-null"), "Should call loader returning null");

          assertEquals("loaded-key-map", map.get("key-map"), "Should call loader for map get");

          assertEquals(
              "loaded-key-function-map",
              map.computeIfAbsent("key-function-map", k -> "computed-" + k),
              "Should not call mapping function if loader returns value");

          assertEquals(
              "computed-key-function-map-null",
              map.computeIfAbsent("key-function-map-null", k -> "computed-" + k),
              "Should call mapping function if loader returns null");

          @SuppressWarnings("unlikely-arg-type")
          var wrongKeyTypeResult = map.get(8);
          assertNull(wrongKeyTypeResult, "Should return null for wrong key type");
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

  private void doRemovalListenerOperations(
      Function<String, CacheBuilder<String, Object>> cacheBuilderFactory) {
    var removalResults = new ConcurrentHashMap<RemovalCause, Map<String, Object>>();

    useCache(
        cacheBuilderFactory
            .apply("test-removal-listener")
            .expireAfterWrite(TTL)
            .removalListener(
                (key, value, cause) ->
                    removalResults.computeIfAbsent(cause, k -> new HashMap<>()).put(key, value))
            .build(),
        cache -> {
          cache.put("key1", "value1");
          cache.put("key2", "value2");
          cache.put("key2", "value22");
          cache.invalidate("key1");

          await().atMost(TTL).until(() -> removalResults.containsKey(RemovalCause.REMOVED));
          assertEquals(
              "value1",
              removalResults.get(RemovalCause.REMOVED).get("key1"),
              "Should call listener with REMOVED cause");

          await().atMost(TTL).until(() -> removalResults.containsKey(RemovalCause.REPLACED));
          assertEquals(
              "value2",
              removalResults.get(RemovalCause.REPLACED).get("key2"),
              "Should call listener with REPLACED cause");

          await()
              .atMost(Duration.ofSeconds(5).plus(TTL))
              .pollDelay(TTL)
              .pollInterval(TTL.dividedBy(2))
              .until(
                  () -> {
                    cache.cleanUp();
                    return removalResults.containsKey(RemovalCause.EXPIRED);
                  });
          assertEquals(
              "value22",
              removalResults.get(RemovalCause.EXPIRED).get("key2"),
              "Should call listener with EXPIRED cause");

          assertEquals(0, cache.estimatedSize(), "Cache should be empty");
        });
  }

  @ParameterizedTest(name = "{0} - GetAll Operations")
  @EnumSource(CacheType.class)
  void testGetAllOperations(CacheType cacheType) {
    doGetAllOperations(cacheType::getCacheBuilder);
  }

  @ParameterizedTest(name = "{0} - PutAll Operations")
  @EnumSource(CacheType.class)
  void testPutAllOperations(CacheType cacheType) {
    assumeTrue(
        hasHPExpire || cacheType != CacheType.REDISSON_NATIVE,
        "redisson-native expireAfterWrite requires Redis HPEXPIRE support");
    doPutAllOperations(cacheType::getCacheBuilder);
  }

  private void doGetAllOperations(
      Function<String, CacheBuilder<String, Object>> cacheBuilderFactory) {
    useCache(
        cacheBuilderFactory
            .apply("test-get-all")
            .build(key -> key.startsWith("load-") ? "loaded-" + key : null),
        cache -> {
          // Existing
          var initial =
              Map.of(
                  "key1", "value1",
                  "key2", "value2");
          cache.putAll(initial);

          assertEquals(
              initial, cache.getAll(initial.keySet()), "Should return all existing values");

          // Mixed existing/missing/loaded
          var mixedKeys = Set.of("key1", "missing1", "load-missing2");
          var expectedMixed =
              Map.of(
                  "key1", "value1",
                  "load-missing2", "loaded-load-missing2");

          assertEquals(
              expectedMixed,
              cache.getAll(mixedKeys),
              "Should return mixed existing/missing/loaded values");

          // All missing
          var allMissing = Set.of("missing3", "missing4");
          assertEquals(
              Map.of(), cache.getAll(allMissing), "Should return empty map for all missing keys");
        });
  }

  private void doPutAllOperations(
      Function<String, CacheBuilder<String, Object>> cacheBuilderFactory) {
    useCache(
        cacheBuilderFactory.apply("test-put-all").expireAfterWrite(TTL).build(),
        cache -> {
          // Initial putAll
          Map<String, Object> batch1 =
              Map.of(
                  "key1", "value1",
                  "key2", "value2");
          cache.putAll(batch1);
          assertEquals(2, cache.estimatedSize(), "Should add all new entries");

          // Update existing and add new
          Map<String, Object> batch2 =
              Map.of(
                  "key2", "value2-updated",
                  "key3", "value3");
          cache.putAll(batch2);
          assertEquals(3, cache.estimatedSize(), "Should update existing and add new");
          assertEquals("value2-updated", cache.get("key2"), "Should update existing entry");

          // Test batch
          Map<String, Object> largeBatch = new HashMap<>();
          IntStream.range(0, 100).forEach(i -> largeBatch.put("batch-key" + i, "value" + i));
          cache.putAll(largeBatch);
          assertEquals(103, cache.estimatedSize(), "Should handle large putAll operations");

          // Verify
          assertEquals("value1", cache.get("key1"), "Original entry should remain");
          assertEquals("value2-updated", cache.get("key2"), "Updated entry should persist");
          assertEquals("value99", cache.get("batch-key99"), "Last batch entry should exist");

          // putAll should expire
          await()
              .atMost(Duration.ofSeconds(2))
              .pollInterval(TTL.dividedBy(2))
              .untilAsserted(
                  () ->
                      assertFalse(
                          cache.asMap().containsKey("key1"), "putAll should expire entries"));
        });
  }

  @ParameterizedTest(name = "{0} - Serialization")
  @EnumSource(CacheType.class)
  void testSerialization(CacheType cacheType) {
    doSerializationOperations(cacheType::getCacheBuilder);
  }

  private void doSerializationOperations(
      Function<String, CacheBuilder<String, Object>> cacheBuilderFactory) {
    useCache(
        cacheBuilderFactory.apply("test-serialization").build(),
        cache -> {
          // Serialize data
          var container1 = new MyContainer("myContainer");
          container1.getDataMap().put("key1", new MyData("myName1"));
          cache.put("test", container1);

          // Deserialize data
          var container2 = (MyContainer) cache.get("test");
          assertEquals(
              container1.getData().getName(),
              container2.getData().getName(),
              "Deserialized data should match");
          assertEquals(
              container1.getDataMap().get("key1").getName(),
              container2.getDataMap().get("key1").getName(),
              "Deserialized data should match");

          // Update data
          container2.getData().setName("myUpdatedContainer");
          container2.getDataMap().put("key1", new MyData("myUpdatedName1"));
          container2.getDataMap().put("key2", new MyData("myName2"));

          // Storing data again is required to update the cache entry
          // (except for Caffeine which does not serialize)
          cache.put("test", container2);

          // Deserialize updated data
          var container3 = (MyContainer) cache.get("test");
          assertEquals(
              container2.getData().getName(),
              container3.getData().getName(),
              "Deserialized updated data should match");
          assertEquals(
              container2.getDataMap().size(),
              container3.getDataMap().size(),
              "Deserialized updated data should match");
          assertEquals(
              container2.getDataMap().get("key1").getName(),
              container3.getDataMap().get("key1").getName(),
              "Deserialized updated data should match");
          assertEquals(
              container2.getDataMap().get("key2").getName(),
              container3.getDataMap().get("key2").getName(),
              "Deserialized updated data should match");
        });
  }

  private static class MyContainer {
    private final MyData data;
    private final Map<String, MyData> dataMap;

    public MyContainer(String name) {
      this.data = new MyData(name);
      this.dataMap = new HashMap<>();
    }

    public MyData getData() {
      return data;
    }

    public Map<String, MyData> getDataMap() {
      return dataMap;
    }
  }

  private static class MyData {
    private String name;

    public MyData(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
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
