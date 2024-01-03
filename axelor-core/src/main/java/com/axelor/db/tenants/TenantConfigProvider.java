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

import com.axelor.auth.db.User;
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
   * Check whether the user has permission to use given tenant.
   *
   * @param user the user to check
   * @param config the tenant configuration to check
   * @return true if user can user this tenant false otherwise
   */
  boolean hasAccess(User user, TenantConfig config);
}
