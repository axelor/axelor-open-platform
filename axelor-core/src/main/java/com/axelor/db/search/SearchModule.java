/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.google.inject.AbstractModule;

/** A Guice module to configure full-text search feature. */
public class SearchModule extends AbstractModule {

  public static final String DEFAULT_DIRECTORY_PROVIDER = "filesystem";
  public static final String DEFAULT_INDEX_BASE = "{user.home}/.axelor/indexes";

  public static boolean isEnabled() {
    return !"none"
        .equalsIgnoreCase(
            AppSettings.get()
                .get(AvailableAppSettings.HIBERNATE_SEARCH_DEFAULT_DIRECTORY_PROVIDER, "none"));
  }

  @Override
  protected void configure() {
    bind(SearchSupport.class).asEagerSingleton();
  }
}
