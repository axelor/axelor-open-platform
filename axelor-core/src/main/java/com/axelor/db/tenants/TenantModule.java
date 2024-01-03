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
package com.axelor.db.tenants;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.google.inject.AbstractModule;

/** A Guice module to provide multi-tenancy support. */
public class TenantModule extends AbstractModule {

  public static boolean isEnabled() {
    return AppSettings.get().getBoolean(AvailableAppSettings.CONFIG_MULTI_TENANCY, false);
  }

  @Override
  protected void configure() {
    bind(TenantSupport.class).asEagerSingleton();
  }
}
