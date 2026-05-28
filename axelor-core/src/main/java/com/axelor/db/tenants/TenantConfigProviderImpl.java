/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.tenants;

import com.axelor.app.AppSettings;
import java.util.List;

/** The default {@link TenantConfigProvider} implementation. */
public class TenantConfigProviderImpl implements TenantConfigProvider {

  @Override
  public TenantConfig find(String tenantId) {
    return TenantConfigImpl.findById(AppSettings.get().getProperties(), tenantId);
  }

  @Override
  public List<TenantConfig> findAll(String host) {
    return TenantConfigImpl.findByHost(AppSettings.get().getProperties(), host);
  }

  @Override
  public List<TenantConfig> findAll() {
    return TenantConfigImpl.findAll(AppSettings.get().getProperties());
  }
}
