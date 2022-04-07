package com.axelor.cache;

import com.axelor.TestingHelpers;
import com.axelor.app.AppSettings;
import com.axelor.test.GuiceModules;
import org.hibernate.cache.jcache.ConfigSettings;

@GuiceModules(HazelcastTest.HazelcastTestModule.class)
public class HazelcastTest extends AbstractBaseCache {

  public static class HazelcastTestModule extends CacheTestModule {

    @Override
    protected void configure() {
      TestingHelpers.resetSettings();

      System.setProperty("hazelcast.ignoreXxeProtectionFailures", "true");
      AppSettings.get()
          .getInternalProperties()
          .put(ConfigSettings.PROVIDER, "com.hazelcast.cache.impl.HazelcastServerCachingProvider");

      super.configure();
    }
  }
}
