/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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

import com.axelor.app.AppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;

/**
 * The default {@link TenantConfigProvider} implementation.
 *
 */
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
	public boolean hasAccess(User user, TenantConfig config) {
		final String roles = config.getTenantRoles();
		if (user == null) {
			return false;
		}
		return StringUtils.isBlank(roles) || AuthUtils.isAdmin(user) || AuthUtils.hasRole(user, roles.split(","));
	}
}
