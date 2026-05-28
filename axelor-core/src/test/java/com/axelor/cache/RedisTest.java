/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache;

import com.axelor.TestingHelpers;
import com.axelor.app.AppSettings;
import com.axelor.cache.redisson.RedissonProvider;
import com.axelor.test.GuiceModules;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import org.hibernate.cfg.Environment;
import org.junit.jupiter.api.AfterAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.embedded.RedisServer;

@GuiceModules(RedisTest.RedisTestModule.class)
public class RedisTest extends AbstractBaseCache {

  private static RedisServer redisServer;
  private static int redisUseCount = 0;

  private static final String REDIS_HOST = "localhost";
  private static final int REDIS_PORT = 6379;
  private static final int TIMEOUT_MS = 2000;

  private static final Logger log = LoggerFactory.getLogger(RedisTest.class);

  public static class RedisTestModule extends CacheTestModule {

    @Override
    protected void configure() {
      TestingHelpers.resetSettings();

      startRedis();

      AppSettings.get()
          .getInternalProperties()
          .put(Environment.CACHE_REGION_FACTORY, "org.redisson.hibernate.RedissonRegionFactory");

      super.configure();
    }
  }

  @AfterAll
  static void tearDown() {
    stopRedis();
  }

  static synchronized void startRedis() {
    ++redisUseCount;

    if (redisServer != null) {
      if (!redisServer.isActive()) {
        redisServer.start();
      }
      return;
    }

    if (isServerRunning()) {
      log.warn("External Redis server is already running on port " + REDIS_PORT);
    } else {
      redisServer = new RedisServer(REDIS_PORT);
      redisServer.start();
    }
  }

  static synchronized void stopRedis() {
    --redisUseCount;

    if (redisUseCount > 0) {
      return;
    }

    if (redisServer != null && !RedissonProvider.isActive()) {
      redisServer.stop();
    }
  }

  private static boolean isServerRunning() {
    try (var socket = new Socket(REDIS_HOST, REDIS_PORT)) {
      socket.setSoTimeout(TIMEOUT_MS);

      try (var out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
          var in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

        out.print("PING\r\n");
        out.flush();

        var response = in.readLine();

        return "+PONG".equals(response);
      }
    } catch (IOException e) {
      return false;
    }
  }
}
