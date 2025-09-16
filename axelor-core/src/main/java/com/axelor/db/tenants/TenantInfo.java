/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.tenants;

import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

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
