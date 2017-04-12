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

import java.util.List;

import com.axelor.auth.db.User;
import com.google.inject.ImplementedBy;

/**
 * The contract to provide {@link TenantConfig}.
 *
 */
@ImplementedBy(TenantConfigProviderImpl.class)
public interface TenantConfigProvider {

	/**
	 * Find {@link TenantConfig} for the given tenant identifier.
	 * 
	 * @param tenantId
	 *            the tenant identifier
	 * @return an instance of {@link TenantConfig}
	 */
	TenantConfig find(String tenantId);

	/**
	 * Find all {@link TenantConfig} for the given hostname.
	 * 
	 * @param host
	 *            the hostname
	 * @return list of all {@link TenantConfig} matching the given hostname
	 */
	List<TenantConfig> findAll(String host);

	/**
	 * Check whether the user has permission to use given tenant.
	 * 
	 * @param user
	 *            the user to check
	 * @param config
	 *            the tenant configuration to check
	 * @return true if user can user this tenant false otherwise
	 */
	boolean hasAccess(User user, TenantConfig config);
}
