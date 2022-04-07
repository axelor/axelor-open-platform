package com.axelor.cache;

import com.axelor.TestingHelpers;
import com.axelor.app.AppSettings;
import com.axelor.test.GuiceModules;
import org.hibernate.cfg.Environment;
import org.junit.jupiter.api.Disabled;

/**
 * `AbstractBaseCache#shouldHitQueryCache` fail with Infinispan : cache isn't hit Moreover
 * Infinispan 13.0.8.Final depends on Caffeine 2.8.4. Caffeine 3+ isn't compatible with Infinispan.
 */
@Disabled
@GuiceModules(InfinispanTest.InfinispanTestModule.class)
public class InfinispanTest extends AbstractBaseCache {

  public static class InfinispanTestModule extends CacheTestModule {

    @Override
    protected void configure() {
      TestingHelpers.resetSettings();

      AppSettings.get().getInternalProperties().put(Environment.CACHE_REGION_FACTORY, "infinispan");

      super.configure();
    }
  }
}
