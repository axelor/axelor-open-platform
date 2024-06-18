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
package com.axelor.i18n;

import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.TypedQuery;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** The database backed {@link ResourceBundle} that loads translations from the axelor database. */
public class I18nBundle extends ResourceBundle {

  private final Locale locale;
  private final Map<String, String> messages = new ConcurrentHashMap<>();

  private boolean loaded;

  public I18nBundle(Locale locale) {
    this.locale = locale;
  }

  @Override
  protected Object handleGetObject(String key) {
    if (StringUtils.isBlank(key)) {
      return key;
    }
    final String result = load().get(key);
    if (StringUtils.isBlank(result)) {
      return key;
    }
    return result;
  }

  @Override
  protected Set<String> handleKeySet() {
    return load().keySet();
  }

  @Override
  public Enumeration<String> getKeys() {
    return Collections.enumeration(load().keySet());
  }

  @Override
  public boolean containsKey(String key) {
    return handleKeySet().contains(key);
  }

  private Map<String, String> load() {
    return loaded ? messages : doLoad();
  }

  private synchronized Map<String, String> doLoad() {

    try {
      JPA.em();
    } catch (Throwable e) {
      return messages;
    }

    final String lang = locale.toLanguageTag();
    final String baseLang = locale.getLanguage();
    final int limit = 100;
    final TypedQuery<Object[]> query =
        JPA.em()
            .createQuery(
                "SELECT self.key, MAX(CASE WHEN self.language = :lang THEN self.message ELSE base.message END) "
                    + "FROM MetaTranslation self "
                    + "LEFT JOIN MetaTranslation base ON base.key = self.key AND base.language = :baseLang "
                    + "WHERE self.message IS NOT NULL AND self.language IN (:lang, :baseLang) "
                    + "GROUP BY self.key "
                    + "ORDER BY self.key",
                Object[].class)
            .setParameter("lang", lang)
            .setParameter("baseLang", baseLang)
            .setFlushMode(FlushModeType.COMMIT)
            .setMaxResults(limit);

    int offset = 0;
    List<Object[]> results;

    do {
      query.setFirstResult(offset);
      results = query.getResultList();
      for (final Object[] result : results) {
        messages.put((String) result[0], (String) result[1]);
      }
      offset += limit;
    } while (results.size() >= limit);

    this.loaded = true;
    return messages;
  }

  public static void invalidate() {
    ResourceBundle.clearCache();
  }
}
