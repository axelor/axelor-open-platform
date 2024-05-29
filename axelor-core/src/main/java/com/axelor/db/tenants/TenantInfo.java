/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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

import java.util.Collections;
import java.util.Map;
import jakarta.annotation.Nullable;

public class TenantInfo {

  private final String tenant;

  private final Map<String, String> tenants;

  private TenantInfo(String tenant, Map<String, String> tenants) {
    this.tenant = tenant;
    this.tenants = tenants;
  }

  public static TenantInfo single(String tenant) {
    return new TenantInfo(tenant, Collections.emptyMap());
  }

  public static TenantInfo multiple(Map<String, String> tenants) {
    return new TenantInfo(null, tenants);
  }

  /**
   * Gets the host-resolved tenant ID, if applicable.
   *
   * @return tenant ID or null
   */
  @Nullable
  public String getHostTenant() {
    return tenant;
  }

  /**
   * Gets the map of tenant IDs to tenant names that are available.
   *
   * @return map of tenant IDs to tenant names
   */
  public Map<String, String> getTenants() {
    return tenants;
  }
}
