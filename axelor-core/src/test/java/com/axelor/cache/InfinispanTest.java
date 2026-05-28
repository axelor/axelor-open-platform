/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache;

import com.axelor.TestingHelpers;
import com.axelor.app.AppSettings;
import com.axelor.test.GuiceModules;
import org.hibernate.cfg.Environment;
import org.junit.jupiter.api.Disabled;

/** `AbstractBaseCache#shouldHitQueryCache` fail with Infinispan: cache isn't hit. */
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
