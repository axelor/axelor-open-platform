/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
package com.axelor.meta.theme;

import com.axelor.auth.db.User;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaTheme;
import com.axelor.meta.db.repo.MetaThemeRepository;
import java.util.List;

public class MetaThemeServiceImpl implements MetaThemeService {

  @Override
  public MetaTheme getTheme(String name, User user) {
    return Beans.get(MetaThemeRepository.class)
        .all()
        .filter("self.name = :name")
        .bind("name", name)
        .fetchOne();
  }

  /**
   * {@inheritDoc}
   *
   * <p>By default, all selectable non archived themes
   */
  @Override
  public List<MetaTheme> getAvailableThemes(User user) {
    return Beans.get(MetaThemeRepository.class)
        .all()
        .filter("self.isSelectable = true AND (self.archived is null OR self.archived = false)")
        .fetch();
  }
}
