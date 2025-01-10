/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaTheme;
import com.axelor.meta.db.repo.MetaThemeRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
   * <p>By default, all selectable non archived themes as well as <code>light</code>/<code>dark
   * </code> and <code>auto</code>.
   */
  @Override
  public List<AvailableTheme> getAvailableThemes(User user) {
    List<AvailableTheme> themes =
        Optional.ofNullable(getMetaThemes(user)).orElse(new ArrayList<>()).stream()
            .map(AvailableTheme::new)
            .collect(Collectors.toList());
    ;

    themes.addAll(Optional.ofNullable(getDefaultThemes(user)).orElse(new ArrayList<>()));

    return themes;
  }

  protected List<MetaTheme> getMetaThemes(User user) {
    return Beans.get(MetaThemeRepository.class)
        .all()
        .filter("self.isSelectable = true AND (self.archived is null OR self.archived = false)")
        .fetch();
  }

  protected List<AvailableTheme> getDefaultThemes(User user) {
    return List.of(
        new AvailableTheme("light", I18n.get("Light")),
        new AvailableTheme("dark", I18n.get("Dark")),
        new AvailableTheme("auto", I18n.get("Auto")));
  }
}
