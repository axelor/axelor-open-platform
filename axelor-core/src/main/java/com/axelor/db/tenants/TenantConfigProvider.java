/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.tenants;

import com.google.inject.ImplementedBy;
import java.util.List;

/** The contract to provide {@link TenantConfig}. */
@ImplementedBy(TenantConfigProviderImpl.class)
public interface TenantConfigProvider {

  /**
   * Find {@link TenantConfig} for the given tenant identifier.
   *
   * @param tenantId the tenant identifier
   * @return an instance of {@link TenantConfig}
   */
  TenantConfig find(String tenantId);

  /**
   * Find all {@link TenantConfig} for the given hostname.
   *
   * @param host the hostname
   * @return list of all {@link TenantConfig} matching the given hostname
   */
  List<TenantConfig> findAll(String host);

  /**
   * Find all {@link TenantConfig}.
   *
   * @return list of all {@link TenantConfig}
   */
  List<TenantConfig> findAll();
}
