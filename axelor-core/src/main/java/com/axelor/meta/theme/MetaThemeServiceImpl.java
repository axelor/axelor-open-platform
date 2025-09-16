/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
