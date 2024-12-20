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
