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

import com.axelor.cache.caffeine.CaffeineCacheBuilder;
import com.axelor.cache.redisson.RedissonCacheBuilder;
import com.axelor.test.GuiceJunit5Test;
import com.axelor.test.GuiceModules;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@GuiceModules({AbstractBaseCache.CacheTestModule.class})
class CacheBuilderTest extends GuiceJunit5Test {

  @BeforeAll
  static void setUp() {
    RedisTest.startRedis();
  }

  @AfterAll
  static void tearDown() {
    RedisTest.stopRedis();
  }

  @Test
  void testCaffeineCache() {
    testCacheOperations(CaffeineCacheBuilder::new);
  }

  @Test
  void testCaffeineCacheLoader() {
    testCacheLoaderOperations(CaffeineCacheBuilder::new);
  }

  @Test
  void testCaffeineExpireAfterWrite() {
    testExpireAfterWrite(CaffeineCacheBuilder::new);
  }

  @Test
  void testCaffeineExpireAfterAccess() {
    testExpireAfterAccess(CaffeineCacheBuilder::new);
  }

  @Test
  void testRedissonCache() {
    testCacheOperations(RedissonCacheBuilder::new);
  }

  @Test
  void testRedissonCacheLoader() {
    testCacheLoaderOperations(RedissonCacheBuilder::new);
  }

  @Test
  void testRedissonExpireAfterWrite() {
    testExpireAfterWrite(RedissonCacheBuilder::new);
  }

  @Test
  void testRedissonExpireAfterAccess() {
    testExpireAfterAccess(RedissonCacheBuilder::new);
  }

  // Test cache without loader
  private void testCacheOperations(
      Function<String, CacheBuilder<String, Object>> cacheBuilderFactory) {
    useCache(
        cacheBuilderFactory.apply("test-cache").build(),
        cache -> {
          assertNotNull(cache, "Cache should be created");

          cache.put("key1", "value1");
          assertEquals("value1", cache.get("key1"), "Should return cached value");

          assertNull(cache.get("key2"), "Should return null for non-existent key");

          cache.invalidate("key1");
          assertNull(cache.get("key1"), "Should return null after invalidation");
        });
  }

  // Test cache with loader
  private void testCacheLoaderOperations(
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

  private void testExpireAfterWrite(
      Function<String, CacheBuilder<String, Object>> cacheBuilderFactory) {
    var expireAfterWrite = Duration.ofMillis(500);

    useCache(
        cacheBuilderFactory.apply("test-write-expiry").expireAfterWrite(expireAfterWrite).build(),
        cache -> {
          cache.put("key1", "value1");
          assertEquals("value1", cache.get("key1"), "Should return cached value");

          await()
              .atMost(expireAfterWrite)
              .pollDelay(expireAfterWrite.dividedBy(2))
              .untilAsserted(
                  () ->
                      assertEquals(
                          "value1", cache.get("key1"), "Should not expire before write timeout"));

          await()
              .atMost(expireAfterWrite)
              .pollDelay(expireAfterWrite.dividedBy(2))
              .untilAsserted(
                  () -> assertNull(cache.get("key1"), "Should expire after write timeout"));
        });
  }

  private void testExpireAfterAccess(
      Function<String, CacheBuilder<String, Object>> cacheBuilderFactory) {
    var expireAfterAccess = Duration.ofMillis(500);

    useCache(
        cacheBuilderFactory
            .apply("test-access-expiry")
            .expireAfterAccess(expireAfterAccess)
            .build(),
        cache -> {
          assertEquals(0, cache.estimatedSize(), "Cache should be empty initially");

          cache.put("key1", "value1");
          assertEquals("value1", cache.get("key1"), "Should return cached value");

          await()
              .atMost(expireAfterAccess)
              .pollDelay(expireAfterAccess.dividedBy(2))
              .untilAsserted(
                  () ->
                      assertEquals(
                          "value1",
                          cache.get("key1"),
                          "Should not expire before last access timeout"));

          assertEquals(1, cache.estimatedSize(), "Cache should contain one entry");

          await()
              .atMost(expireAfterAccess.multipliedBy(2))
              .pollDelay(expireAfterAccess)
              .untilAsserted(
                  () -> assertNull(cache.get("key1"), "Should expire after last access timeout"));
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
