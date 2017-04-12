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

import com.google.inject.AbstractModule;

/**
 * A Guice module to provide multi-tenancy support.
 *
 */
public class TenantModule extends AbstractModule {

	public static final String CONFIG_MULTI_TENANCY = "application.multi_tenancy";

	@Override
	protected void configure() {
		bind(TenantSupport.class).asEagerSingleton();
	}
}
