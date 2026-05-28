/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.service.menu;

import com.axelor.common.ObjectUtils;
import com.axelor.db.JPA;
import com.axelor.meta.db.MetaMenu;
import jakarta.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
        """
        SELECT self FROM MetaMenu self \
        LEFT JOIN FETCH self.action \
        LEFT JOIN FETCH self.parent \
        """;

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
