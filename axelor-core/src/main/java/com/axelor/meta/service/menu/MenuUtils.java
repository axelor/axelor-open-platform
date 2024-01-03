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
package com.axelor.meta.service.menu;

import com.axelor.common.ObjectUtils;
import com.axelor.db.JPA;
import com.axelor.meta.db.MetaMenu;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.persistence.TypedQuery;

public class MenuUtils {

  /**
   * Fetch {@link MetaMenu} for the given names. It also makes sure the remove the overwritten menus
   * based on their priorities
   *
   * @return list of {@link MetaMenu}
   */
  public static List<MetaMenu> fetchMetaMenu(List<String> names) {
    final Map<String, Object> params = new HashMap<>();
    String queryString =
        "SELECT self FROM MetaMenu self "
            + "LEFT JOIN FETCH self.action "
            + "LEFT JOIN FETCH self.parent ";

    if (ObjectUtils.notEmpty(names)) {
      queryString += "WHERE self.name IN (:names) ";
      params.put("names", names);
    }
    queryString += "ORDER BY COALESCE(self.priority, 0) DESC, self.id";

    final TypedQuery<MetaMenu> query = JPA.em().createQuery(queryString, MetaMenu.class);
    params.forEach(query::setParameter);
    return query.getResultList().stream()
        .filter(distinctByKey(MetaMenu::getName)) // Skip overwritten menus
        .collect(Collectors.toList());
  }

  public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
    Map<Object, Boolean> seen = new ConcurrentHashMap<>();
    return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
  }
}
