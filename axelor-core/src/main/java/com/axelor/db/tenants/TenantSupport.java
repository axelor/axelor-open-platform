/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.db.tenants;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Provides configured {@link TenantConfigProvider}. */
@Singleton
class TenantSupport {

  private static TenantSupport INSTANCE;

  private TenantConfigProvider confixProvider;

  @Inject
  private TenantSupport(TenantConfigProvider configProvider) {
    this.confixProvider = configProvider;
    TenantSupport.INSTANCE = this;
  }

  public static TenantSupport get() {
    if (INSTANCE == null) {
      throw new RuntimeException("Multi-tenant support is not configured properly.");
    }
    return INSTANCE;
  }

  public TenantConfigProvider getConfigProvider() {
    return confixProvider;
  }
}
