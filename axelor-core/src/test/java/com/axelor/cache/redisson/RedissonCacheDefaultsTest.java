/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.redisson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import org.hibernate.cache.jcache.internal.JCacheRegionFactory;
import org.junit.jupiter.api.Test;
import org.redisson.hibernate.RedissonRegionFactory;

class RedissonCacheDefaultsTest {

  @Test
  void shouldApplyDefaultsForRedissonRegionFactory() {
    var properties = new Properties();

    RedissonCacheDefaults.applyHibernateRegionDefaults(
        AxelorRedissonRegionFactory.class.getName(), properties);

    assertEquals("1000", properties.get("hibernate.cache.redisson.entity.eviction.max_entries"));
    assertEquals(
        "600000", properties.get("hibernate.cache.redisson.entity.expiration.time_to_live"));
    assertEquals(
        "1000", properties.get("hibernate.cache.redisson.collection.eviction.max_entries"));
    assertEquals("1000", properties.get("hibernate.cache.redisson.naturalid.eviction.max_entries"));

    assertEquals("100", properties.get("hibernate.cache.redisson.query.eviction.max_entries"));
    assertEquals(
        "600000", properties.get("hibernate.cache.redisson.query.expiration.time_to_live"));

    // No expiration on update timestamps region
    assertNull(properties.get("hibernate.cache.redisson.timestamps.eviction.max_entries"));
    assertNull(properties.get("hibernate.cache.redisson.timestamps.expiration.time_to_live"));

    assertEquals(
        "500",
        properties.get("hibernate.cache.redisson.com.axelor.auth.db.User.eviction.max_entries"));
    assertEquals(
        "0",
        properties.get("hibernate.cache.redisson.com.axelor.auth.db.User.expiration.time_to_live"));
    assertEquals(
        "86400000",
        properties.get(
            "hibernate.cache.redisson.com.axelor.auth.db.User.expiration.max_idle_time"));
    assertEquals(
        "200",
        properties.get("hibernate.cache.redisson.com.axelor.auth.db.Group.eviction.max_entries"));
    assertEquals(
        "200",
        properties.get("hibernate.cache.redisson.com.axelor.auth.db.Role.eviction.max_entries"));
    assertEquals(
        "10000",
        properties.get(
            "hibernate.cache.redisson.com.axelor.auth.db.Permission.eviction.max_entries"));

    // Auth association collection regions, sized to mirror their owning entity
    assertEquals(
        "500",
        properties.get(
            "hibernate.cache.redisson.com.axelor.auth.db.User.roles.eviction.max_entries"));
    assertEquals(
        "0",
        properties.get(
            "hibernate.cache.redisson.com.axelor.auth.db.User.roles.expiration.time_to_live"));
    assertEquals(
        "86400000",
        properties.get(
            "hibernate.cache.redisson.com.axelor.auth.db.User.roles.expiration.max_idle_time"));
    assertEquals(
        "500",
        properties.get(
            "hibernate.cache.redisson.com.axelor.auth.db.User.permissions.eviction.max_entries"));
    assertEquals(
        "200",
        properties.get(
            "hibernate.cache.redisson.com.axelor.auth.db.Group.roles.eviction.max_entries"));
    assertEquals(
        "200",
        properties.get(
            "hibernate.cache.redisson.com.axelor.auth.db.Group.permissions.eviction.max_entries"));
    assertEquals(
        "200",
        properties.get(
            "hibernate.cache.redisson.com.axelor.auth.db.Role.permissions.eviction.max_entries"));
  }

  @Test
  void shouldOnlyApplyTimeToLiveForNativeRegionFactory() {
    var properties = new Properties();

    RedissonCacheDefaults.applyHibernateRegionDefaults(
        AxelorRedissonRegionNativeFactory.class.getName(), properties);

    assertEquals(
        "600000", properties.get("hibernate.cache.redisson.entity.expiration.time_to_live"));
    assertEquals(
        "600000", properties.get("hibernate.cache.redisson.query.expiration.time_to_live"));
    assertEquals(
        "86400000",
        properties.get("hibernate.cache.redisson.com.axelor.auth.db.User.expiration.time_to_live"));
    assertEquals(
        "86400000",
        properties.get(
            "hibernate.cache.redisson.com.axelor.auth.db.User.roles.expiration.time_to_live"));

    // The native region factory rejects non-zero max_entries and max_idle_time settings.
    assertTrue(
        properties.keySet().stream()
            .map(Object::toString)
            .noneMatch(
                key ->
                    key.endsWith(RedissonRegionFactory.MAX_ENTRIES_SUFFIX)
                        || key.endsWith(RedissonRegionFactory.MAX_IDLE_SUFFIX)));
  }

  @Test
  void shouldNotOverrideExistingSettings() {
    var properties = new Properties();
    properties.put("hibernate.cache.redisson.entity.eviction.max_entries", "5000");
    properties.put(
        "hibernate.cache.redisson.com.axelor.auth.db.User.expiration.max_idle_time", "60000");

    RedissonCacheDefaults.applyHibernateRegionDefaults(
        AxelorRedissonRegionFactory.class.getName(), properties);

    assertEquals("5000", properties.get("hibernate.cache.redisson.entity.eviction.max_entries"));
    assertEquals(
        "60000",
        properties.get(
            "hibernate.cache.redisson.com.axelor.auth.db.User.expiration.max_idle_time"));
    // Other defaults are still applied
    assertEquals(
        "600000", properties.get("hibernate.cache.redisson.entity.expiration.time_to_live"));
  }

  @Test
  void shouldIgnoreNonRedissonRegionFactories() {
    var properties = new Properties();

    RedissonCacheDefaults.applyHibernateRegionDefaults(
        JCacheRegionFactory.class.getName(), properties);
    RedissonCacheDefaults.applyHibernateRegionDefaults("some.unknown.Factory", properties);

    assertTrue(properties.isEmpty());
  }

  @Test
  void shouldSupportPlainRedissonRegionFactory() {
    var properties = new Properties();

    RedissonCacheDefaults.applyHibernateRegionDefaults(
        RedissonRegionFactory.class.getName(), properties);

    assertFalse(properties.isEmpty());
  }
}
