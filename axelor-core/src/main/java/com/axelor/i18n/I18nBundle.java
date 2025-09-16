/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.i18n;

import com.axelor.cache.AxelorCache;
import com.axelor.cache.CacheBuilder;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.TypedQuery;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/** The database backed {@link ResourceBundle} that loads translations from the axelor database. */
public class I18nBundle extends ResourceBundle {

  private final Locale locale;
  private final AxelorCache<String, String> messages;

  private boolean loaded;

  public I18nBundle(Locale locale) {
    this.locale = locale;
    messages = CacheBuilder.newBuilder(locale.toLanguageTag()).build();

    // With distributed cache, we may have messages previously loaded.
    loaded = messages.estimatedSize() > 0;
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
    return loaded ? messages.asMap() : doLoad();
  }

  private synchronized Map<String, String> doLoad() {
    final EntityManager em;

    try {
      em = JPA.em();
    } catch (Throwable e) {
      return messages.asMap();
    }

    final String lang = locale.toLanguageTag();
    final String baseLang = locale.getLanguage();
    final int limit = 100;
    final TypedQuery<Object[]> query =
        em.createQuery(
                """
                SELECT self.key, MAX(CASE WHEN self.language = :lang THEN self.message ELSE base.message END) \
                FROM MetaTranslation self \
                LEFT JOIN MetaTranslation base ON base.key = self.key AND base.language = :baseLang \
                WHERE self.message IS NOT NULL AND self.language IN (:lang, :baseLang) \
                GROUP BY self.key \
                ORDER BY self.key""",
                Object[].class)
            .setParameter("lang", lang)
            .setParameter("baseLang", baseLang)
            .setFlushMode(FlushModeType.COMMIT)
            .setMaxResults(limit);

    int offset = 0;
    List<Object[]> results;

    // Clear the cache that might be stored externally
    messages.invalidateAll();

    do {
      query.setFirstResult(offset);
      results = query.getResultList();
      for (final Object[] result : results) {
        messages.put((String) result[0], (String) result[1]);
      }
      offset += limit;
    } while (results.size() >= limit);

    this.loaded = true;
    return messages.asMap();
  }

  public static void invalidate() {
    ResourceBundle.clearCache();
  }
}
