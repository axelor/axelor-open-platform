/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.redisson;

import com.axelor.auth.db.Group;
import com.axelor.auth.db.Permission;
import com.axelor.auth.db.Role;
import com.axelor.auth.db.User;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.redisson.hibernate.RedissonRegionFactory;
import org.redisson.hibernate.RedissonRegionNativeFactory;

/**
 * Default Hibernate second-level cache region settings for Redisson region factories.
 *
 * <p>This mirrors the Caffeine defaults defined in {@code application.conf}. Any {@code
 * hibernate.cache.redisson.*} settings defined in the application configuration take precedence.
 */
public final class RedissonCacheDefaults {

  private static final List<String> DOMAIN_DATA_REGION_GROUPS =
      List.of(
          RedissonRegionFactory.ENTITY_DEF,
          RedissonRegionFactory.COLLECTION_DEF,
          RedissonRegionFactory.NATURAL_ID_DEF);

  // Mirror of "default": 1000 objects, 10 minutes time-to-live
  private static final String DEFAULT_MAX_ENTRIES = "1000";
  private static final String DEFAULT_TTL = String.valueOf(TimeUnit.MINUTES.toMillis(10));

  // Mirror of "default-query-results-region"
  private static final String QUERY_RESULTS_MAX_ENTRIES = "100";

  // Dedicated regions for the frequently-read, rarely-changed entities,
  // expiring on 24-hour idle time instead of time-to-live
  private static final String AUTH_MAX_IDLE = String.valueOf(TimeUnit.HOURS.toMillis(24));

  private RedissonCacheDefaults() {}

  /**
   * Applies default cache region settings if the given region factory is Redisson-based.
   *
   * <p>Settings already present in the given properties are left untouched.
   *
   * @param regionFactoryClassName the configured {@code hibernate.cache.region.factory_class}
   * @param properties the Hibernate properties
   */
  public static void applyHibernateRegionDefaults(
      String regionFactoryClassName, Properties properties) {
    final Class<?> factoryClass;

    try {
      factoryClass = Class.forName(regionFactoryClassName);
    } catch (ClassNotFoundException e) {
      return;
    }

    if (!RedissonRegionFactory.class.isAssignableFrom(factoryClass)) {
      return;
    }

    if (RedissonRegionNativeFactory.class.isAssignableFrom(factoryClass)) {
      applyNativeDefaults(properties);
    } else {
      applyDefaults(properties);
    }
  }

  private static void applyDefaults(Properties properties) {
    for (String group : DOMAIN_DATA_REGION_GROUPS) {
      putDefault(properties, group, RedissonRegionFactory.MAX_ENTRIES_SUFFIX, DEFAULT_MAX_ENTRIES);
      putDefault(properties, group, RedissonRegionFactory.TTL_SUFFIX, DEFAULT_TTL);
    }

    putDefault(
        properties,
        RedissonRegionFactory.QUERY_DEF,
        RedissonRegionFactory.MAX_ENTRIES_SUFFIX,
        QUERY_RESULTS_MAX_ENTRIES);
    putDefault(
        properties, RedissonRegionFactory.QUERY_DEF, RedissonRegionFactory.TTL_SUFFIX, DEFAULT_TTL);

    putAuthEntityDefaults(properties, User.class.getName(), "500");
    putAuthEntityDefaults(properties, Group.class.getName(), "200");
    putAuthEntityDefaults(properties, Role.class.getName(), "200");
    putAuthEntityDefaults(properties, Permission.class.getName(), "10000");
  }

  /**
   * The native variant relies on Redis-side per-entry expiration and rejects non-zero {@code
   * eviction.max_entries} and {@code expiration.max_idle_time} settings, so only time-to-live
   * defaults are applied. The dedicated entity regions use a 24-hour time-to-live as the closest
   * approximation of the 24-hour idle expiration.
   */
  private static void applyNativeDefaults(Properties properties) {
    for (String group : DOMAIN_DATA_REGION_GROUPS) {
      putDefault(properties, group, RedissonRegionFactory.TTL_SUFFIX, DEFAULT_TTL);
    }

    putDefault(
        properties, RedissonRegionFactory.QUERY_DEF, RedissonRegionFactory.TTL_SUFFIX, DEFAULT_TTL);

    for (String region :
        List.of(
            User.class.getName(),
            Group.class.getName(),
            Role.class.getName(),
            Permission.class.getName())) {
      putDefault(properties, region, RedissonRegionFactory.TTL_SUFFIX, AUTH_MAX_IDLE);
    }
  }

  private static void putAuthEntityDefaults(
      Properties properties, String region, String maxEntries) {
    putDefault(properties, region, RedissonRegionFactory.MAX_ENTRIES_SUFFIX, maxEntries);
    // No time-to-live: a zero value prevents falling back to the entity group default
    putDefault(properties, region, RedissonRegionFactory.TTL_SUFFIX, "0");
    putDefault(properties, region, RedissonRegionFactory.MAX_IDLE_SUFFIX, AUTH_MAX_IDLE);
  }

  private static void putDefault(
      Properties properties, String region, String suffix, String value) {
    properties.putIfAbsent(RedissonRegionFactory.CONFIG_PREFIX + region + suffix, value);
  }
}
