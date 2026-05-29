/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration;
import com.github.benmanes.caffeine.jcache.configuration.TypesafeConfigurator;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the Caffeine L2 cache settings in {@code application.conf} are parsed and resolved
 * to the intended per-region expiration.
 */
class CaffeineConfigTest {

  private CaffeineConfiguration<?, ?> region(String name) {
    Config config = ConfigFactory.load();
    return TypesafeConfigurator.from(config, name)
        .orElseThrow(() -> new AssertionError("Region not found in application.conf: " + name));
  }

  @Test
  void defaultRegionExpiresAfterTenMinutes() {
    CaffeineConfiguration<?, ?> cfg = region("default");
    assertTrue(cfg.getExpireAfterWrite().isPresent());
    assertEquals(
        Duration.ofMinutes(10).toNanos(),
        cfg.getExpireAfterWrite().getAsLong(),
        "default after-write should be 10m");
    assertEquals(1000, cfg.getMaximumSize().getAsLong());
  }

  @Test
  void updateTimestampsRegionNeverExpiresAndIsUnbounded() {
    CaffeineConfiguration<?, ?> cfg = region("default-update-timestamps-region");
    assertFalse(cfg.getExpireAfterWrite().isPresent(), "after-write should be unset (null)");
    assertFalse(cfg.getExpireAfterAccess().isPresent());
    assertFalse(cfg.getMaximumSize().isPresent(), "maximum.size should be unset (null)");
  }

  @Test
  void referenceEntityRegionsUseAccessExpirationOnly() {
    for (String name :
        new String[] {
          "com.axelor.auth.db.User",
          "com.axelor.auth.db.Group",
          "com.axelor.auth.db.Role",
          "com.axelor.auth.db.Permission"
        }) {
      CaffeineConfiguration<?, ?> cfg = region(name);
      assertTrue(cfg.getExpireAfterAccess().isPresent(), name + ": after-access should be set");
      assertEquals(
          Duration.ofHours(24).toNanos(),
          cfg.getExpireAfterAccess().getAsLong(),
          name + ": after-access should be 24h");
      assertFalse(
          cfg.getExpireAfterWrite().isPresent(),
          name + ": inherited after-write must be unset so only after-access governs eviction");

      long expectedSize =
          switch (name) {
            case "com.axelor.auth.db.User" -> 500;
            case "com.axelor.auth.db.Group", "com.axelor.auth.db.Role" -> 200;
            case "com.axelor.auth.db.Permission" -> 10000;
            default -> throw new IllegalArgumentException("Unknown region: " + name);
          };
      assertEquals(
          expectedSize,
          cfg.getMaximumSize().getAsLong(),
          name + ": maximum size should be " + expectedSize);
    }
  }

  @Test
  void durationsParseToExpectedUnit() {
    Config config = ConfigFactory.load();
    long ms =
        config.getDuration(
            "caffeine.jcache.default.policy.eager-expiration.after-write", TimeUnit.MILLISECONDS);
    assertEquals(Duration.ofMinutes(10).toMillis(), ms);
  }
}
