/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.tenants;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/** Provides configured {@link TenantConfigProvider}. */
@Singleton
class TenantSupport {

  private static TenantSupport INSTANCE;

  private TenantConfigProvider configProvider;

  @Inject
  private TenantSupport(TenantConfigProvider configProvider) {
    this.configProvider = configProvider;
    TenantSupport.INSTANCE = this;
  }

  public static TenantSupport get() {
    if (INSTANCE == null) {
      throw new RuntimeException("Multi-tenant support is not configured properly.");
    }
    return INSTANCE;
  }

  public TenantConfigProvider getConfigProvider() {
    return configProvider;
  }
}
