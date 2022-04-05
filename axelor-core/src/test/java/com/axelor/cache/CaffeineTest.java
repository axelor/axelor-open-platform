package com.axelor.cache;

import com.axelor.TestingHelpers;
import com.axelor.app.AppSettings;
import com.axelor.test.GuiceModules;
import org.hibernate.cache.jcache.ConfigSettings;

@GuiceModules(CaffeineTest.CaffeineTestModule.class)
public class CaffeineTest extends AbstractBaseCache {

  public static class CaffeineTestModule extends CacheTestModule {

    @Override
    protected void configure() {
      TestingHelpers.resetSettings();

      AppSettings.get()
          .getProperties()
          .put(
              ConfigSettings.PROVIDER,
              "com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider");

      super.configure();
    }
  }
}
