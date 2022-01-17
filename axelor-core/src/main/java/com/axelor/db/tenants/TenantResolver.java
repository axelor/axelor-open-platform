/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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

import javax.servlet.http.HttpSession;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/** The tenant identifier resolver. */
public class TenantResolver implements CurrentTenantIdentifierResolver {

  static final ThreadLocal<String> CURRENT_HOST = new ThreadLocal<>();
  static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

  private static boolean enabled;

  public TenantResolver() {
    enabled = TenantModule.isEnabled();
  }

  public static String currentTenantIdentifier() {
    if (!enabled) return null;
    final String tenant = CURRENT_TENANT.get();
    return tenant == null ? TenantConfig.DEFAULT_TENANT_ID : tenant;
  }

  public static String currentTenantHost() {
    if (!enabled) return null;
    final String tenant = CURRENT_HOST.get();
    return tenant == null ? null : tenant;
  }

  public static boolean isCurrentTenantSession(HttpSession session) {
    if (session == null) return false;
    return !enabled
        || CURRENT_TENANT.get() == null
        || CURRENT_TENANT
            .get()
            .equals(session.getAttribute(AbstractTenantFilter.SESSION_KEY_TENANT_ID));
  }

  @Override
  public String resolveCurrentTenantIdentifier() {
    return currentTenantIdentifier();
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return true;
  }
}
