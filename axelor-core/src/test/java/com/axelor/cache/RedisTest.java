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

import com.axelor.TestingHelpers;
import com.axelor.app.AppSettings;
import com.axelor.test.GuiceModules;
import java.io.IOException;
import java.net.Socket;
import org.hibernate.cfg.Environment;
import org.junit.jupiter.api.AfterAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.embedded.RedisServer;

@GuiceModules(RedisTest.RedisTestModule.class)
public class RedisTest extends AbstractBaseCache {

  private static RedisServer redisServer;

  private static final int REDIS_PORT = 6379;

  private static final Logger log = LoggerFactory.getLogger(RedisTest.class);

  public static class RedisTestModule extends CacheTestModule {

    static void startRedis() {
      redisServer = new RedisServer(REDIS_PORT);
      redisServer.start();
    }

    private boolean isServerRunning() {
      try (final var socket = new Socket("localhost", REDIS_PORT)) {
        return true;
      } catch (IOException e) {
        return false;
      }
    }

    @Override
    protected void configure() {
      TestingHelpers.resetSettings();

      // start redis
      if (isServerRunning()) {
        log.warn("External Redis server is already running on port " + REDIS_PORT);
      } else {
        startRedis();
      }

      AppSettings.get()
          .getInternalProperties()
          .put(Environment.CACHE_REGION_FACTORY, "org.redisson.hibernate.RedissonRegionFactory");

      super.configure();
    }
  }

  @AfterAll
  static void tearDown() {
    if (redisServer != null) {
      redisServer.stop();
    }
  }
}
