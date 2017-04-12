/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import com.axelor.app.AppSettings;

/**
 * The tenant identifier resolver.
 *
 */
public class TenantResolver implements CurrentTenantIdentifierResolver {

	static final ThreadLocal<String> CURRENT_HOST = new ThreadLocal<>();
	static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
	
	private static boolean enabled;

	public TenantResolver() {
		enabled = AppSettings.get().getBoolean(TenantModule.CONFIG_MULTI_TENANCY, false);
	}
	
	public static String currentTenantIdentifier() {
		if (!enabled) {
			return null;
		}
		final String tenant = CURRENT_TENANT.get();
		if (tenant == null) {
			return TenantConfig.DEFAULT_TENANT_ID;
		}
		return tenant;
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
