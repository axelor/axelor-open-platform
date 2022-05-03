/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
