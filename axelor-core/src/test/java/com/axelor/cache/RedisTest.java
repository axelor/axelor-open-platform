package com.axelor.cache;

import com.axelor.app.AppSettings;
import com.axelor.test.GuiceModules;
import org.hibernate.cfg.Environment;
import org.junit.jupiter.api.AfterAll;
import redis.embedded.RedisServer;

@GuiceModules(RedisTest.RedisTestModule.class)
public class RedisTest extends AbstractBaseCache {

  private static RedisServer redisServer;

  public static class RedisTestModule extends CacheTestModule {

    static void startRedis() {
      redisServer = new RedisServer(6379);
      redisServer.start();
    }

    @Override
    protected void configure() {
      resetSettings();

      // start redis
      startRedis();

      AppSettings.get()
          .getProperties()
          .put(Environment.CACHE_REGION_FACTORY, "org.redisson.hibernate.RedissonRegionFactory");

      super.configure();
    }
  }

  @AfterAll
  static void tearDown() {
    redisServer.stop();
  }
}
