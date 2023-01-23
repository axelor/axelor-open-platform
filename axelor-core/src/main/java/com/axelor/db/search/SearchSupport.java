/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.db.search;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.hibernate.search.cfg.SearchMapping;

@Singleton
class SearchSupport {

  private static SearchSupport instance;

  private SearchMappingContributor contributor;

  @Inject
  public SearchSupport(SearchMappingContributor contributor) {
    instance = this;
    this.contributor = contributor;
  }

  public static SearchSupport get() {
    if (instance == null) {
      throw new RuntimeException("Full-text search support is not configured properly.");
    }
    return instance;
  }

  public void contribute(SearchMapping mapping) {
    contributor.contribute(mapping);
  }
}
