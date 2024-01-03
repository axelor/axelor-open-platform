/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
