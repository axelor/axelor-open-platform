/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.db.repo;

import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.Query;
import com.axelor.meta.db.MetaTheme;
import com.google.common.primitives.Longs;
import java.util.List;

public class MetaThemeRepository extends JpaRepository<MetaTheme> {

  public MetaThemeRepository() {
    super(MetaTheme.class);
  }

  public List<MetaTheme> findByName(String name) {
    return Query.of(MetaTheme.class).filter("self.name = :name").bind("name", name).fetch();
  }

  public String fromIdentifierToName(String identifier) {
    Long id = identifier != null ? Longs.tryParse(identifier) : null;
    final MetaTheme theme = id == null ? null : find(id);
    return theme == null ? identifier : theme.getName();
  }

  @Override
  public void remove(MetaTheme entity) {
    if (entity.getIsSelectable()) {
      JPA.em()
          .createQuery("UPDATE User set theme = null where theme = :id")
          .setParameter("id", entity.getId().toString())
          .executeUpdate();
    }

    super.remove(entity);
  }
}
