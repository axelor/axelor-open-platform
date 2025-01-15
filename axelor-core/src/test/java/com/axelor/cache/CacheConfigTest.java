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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.TestingHelpers;
import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CacheConfigTest {

  @BeforeAll
  static void setup() {
    TestingHelpers.resetSettings();
  }

  @AfterEach
  void tearDown() {
    TestingHelpers.resetSettings();
  }

  private void setProperty(String key, String value) {
    var settings = AppSettings.get();
    settings.getInternalProperties().put(key, value);
  }

  @Test
  void testAppCacheConfig() {
    setProperty(AvailableAppSettings.APPLICATION_CACHE_PROVIDER, "redisson");
    setProperty(AvailableAppSettings.APPLICATION_CACHE_CONFIG_PATH, "redisson.yaml");

    var appCacheProvider = CacheConfig.getAppCacheProvider();
    assertEquals("redisson", appCacheProvider.get().getProvider());
    assertEquals("redisson.yaml", appCacheProvider.get().getConfig().get("path"));

    var hibernateCacheProvider = CacheConfig.getHibernateCacheProvider();
    assertEquals("redisson", hibernateCacheProvider.get().getProvider());
    assertEquals("redisson.yaml", hibernateCacheProvider.get().getConfig().get("path"));

    var shiroCacheProvider = CacheConfig.getShiroCacheProvider();
    assertEquals("redisson", shiroCacheProvider.get().getProvider());
    assertEquals("redisson.yaml", shiroCacheProvider.get().getConfig().get("path"));
  }

  @Test
  void testHibernateCacheConfig() {
    setProperty(AvailableAppSettings.APPLICATION_CACHE_HIBERNATE_PROVIDER, "redisson");
    setProperty(
        AvailableAppSettings.APPLICATION_CACHE_HIBERNATE_CONFIG_PATH, "redisson-hibernate.yaml");

    var appCacheProvider = CacheConfig.getAppCacheProvider();
    assertFalse(appCacheProvider.isPresent());

    var hibernateCacheProvider = CacheConfig.getHibernateCacheProvider();
    assertEquals("redisson", hibernateCacheProvider.get().getProvider());
    assertEquals("redisson-hibernate.yaml", hibernateCacheProvider.get().getConfig().get("path"));

    var shiroCacheProvider = CacheConfig.getShiroCacheProvider();
    assertFalse(shiroCacheProvider.isPresent());
  }

  @Test
  void testShiroCacheConfig() {
    setProperty(AvailableAppSettings.APPLICATION_CACHE_SHIRO_PROVIDER, "redisson");
    setProperty(AvailableAppSettings.APPLICATION_CACHE_SHIRO_CONFIG_PATH, "redisson-shiro.yaml");

    var appCacheProvider = CacheConfig.getAppCacheProvider();
    assertFalse(appCacheProvider.isPresent());

    var hibernateCacheProvider = CacheConfig.getHibernateCacheProvider();
    assertFalse(hibernateCacheProvider.isPresent());

    var shiroCacheProvider = CacheConfig.getShiroCacheProvider();
    assertEquals("redisson", shiroCacheProvider.get().getProvider());
    assertEquals("redisson-shiro.yaml", shiroCacheProvider.get().getConfig().get("path"));
  }

  @Test
  void testOverridesCacheConfig() {
    setProperty(AvailableAppSettings.APPLICATION_CACHE_PROVIDER, "redisson");
    setProperty(AvailableAppSettings.APPLICATION_CACHE_CONFIG_PATH, "redisson.yaml");
    setProperty(
        AvailableAppSettings.APPLICATION_CACHE_HIBERNATE_CONFIG_PATH, "redisson-hibernate.yaml");
    setProperty(AvailableAppSettings.APPLICATION_CACHE_SHIRO_CONFIG_PATH, "redisson-shiro.yaml");

    var appCacheProvider = CacheConfig.getAppCacheProvider();
    assertEquals("redisson", appCacheProvider.get().getProvider());
    assertEquals("redisson.yaml", appCacheProvider.get().getConfig().get("path"));

    var hibernateCacheProvider = CacheConfig.getHibernateCacheProvider();
    assertEquals("redisson", hibernateCacheProvider.get().getProvider());
    assertEquals("redisson-hibernate.yaml", hibernateCacheProvider.get().getConfig().get("path"));

    var shiroCacheProvider = CacheConfig.getShiroCacheProvider();
    assertEquals("redisson", shiroCacheProvider.get().getProvider());
    assertEquals("redisson-shiro.yaml", shiroCacheProvider.get().getConfig().get("path"));
  }

  @Test
  void testEmptyConfig() {
    setProperty(AvailableAppSettings.APPLICATION_CACHE_PROVIDER, "redisson");

    var provider = CacheConfig.getAppCacheProvider();
    assertEquals("redisson", provider.get().getProvider());
    assertTrue(provider.get().getConfig().isEmpty());
  }

  @Test
  void testMissingProvider() {
    setProperty(AvailableAppSettings.APPLICATION_CACHE_CONFIG_PATH, "config.yaml");

    var provider = CacheConfig.getAppCacheProvider();
    assertFalse(provider.isPresent());
  }

  @Test
  void testAppCacheEmbeddedConfig() {
    final var singleServerAddressKey = "singleServerConfig.address";
    final var singleServerAddress = "redis://127.0.0.1:6379";

    setProperty(AvailableAppSettings.APPLICATION_CACHE_PROVIDER, "redisson");
    setProperty(
        AvailableAppSettings.APPLICATION_CACHE_CONFIG_PREFIX + singleServerAddressKey,
        singleServerAddress);

    var appCacheProvider = CacheConfig.getAppCacheProvider();
    assertEquals("redisson", appCacheProvider.get().getProvider());
    assertEquals(
        singleServerAddress, appCacheProvider.get().getConfig().get(singleServerAddressKey));
    assertNull(appCacheProvider.get().getConfig().get("path"));

    var hibernateCacheProvider = CacheConfig.getHibernateCacheProvider();
    assertEquals("redisson", hibernateCacheProvider.get().getProvider());
    assertEquals(
        singleServerAddress, hibernateCacheProvider.get().getConfig().get(singleServerAddressKey));
    assertNull(hibernateCacheProvider.get().getConfig().get("path"));

    var shiroCacheProvider = CacheConfig.getShiroCacheProvider();
    assertEquals("redisson", shiroCacheProvider.get().getProvider());
    assertEquals(
        singleServerAddress, shiroCacheProvider.get().getConfig().get(singleServerAddressKey));
    assertNull(shiroCacheProvider.get().getConfig().get("path"));
  }
}
