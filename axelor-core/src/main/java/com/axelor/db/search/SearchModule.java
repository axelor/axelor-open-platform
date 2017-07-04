/*
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
package com.axelor.db.search;

import com.google.inject.AbstractModule;

/**
 * A Guice module to configure full-text search feature.
 *
 */
public class SearchModule extends AbstractModule {

	public static final String CONFIG_DIRECTORY_PROVIDER = "hibernate.search.default.directory_provider";
	public static final String CONFIG_INDEX_BASE = "hibernate.search.default.indexBase";

	public static final String DEFAULT_DIRECTORY_PROVIDER = "filesystem";
	public static final String DEFAULT_INDEX_BASE = "{user.home}/.axelor/indexes";

	@Override
	protected void configure() {
		bind(SearchSupport.class).asEagerSingleton();
	}
}
