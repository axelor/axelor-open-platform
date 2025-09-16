/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
