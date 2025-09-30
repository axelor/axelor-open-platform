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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The database backed {@link ResourceBundle} that loads translations from the axelor database. */
public class I18nBundle extends ResourceBundle {

  private final String languageTag;

  private static final AxelorCache<String, Map<String, String>> messages =
      CacheBuilder.newBuilder("messages").build(I18nBundle::loadMessages);

  private static final Logger log = LoggerFactory.getLogger(I18nBundle.class);

  public I18nBundle(Locale locale) {
    this.languageTag = locale.toLanguageTag();
  }

  @Override
  protected Object handleGetObject(String key) {
    if (StringUtils.isBlank(key)) {
      return key;
    }
    final String result = getMessages().get(key);
    if (StringUtils.isBlank(result)) {
      return key;
    }
    return result;
  }

  @Override
  protected Set<String> handleKeySet() {
    return getMessages().keySet();
  }

  @Override
  public Enumeration<String> getKeys() {
    return Collections.enumeration(getMessages().keySet());
  }

  @Override
  public boolean containsKey(String key) {
    return handleKeySet().contains(key);
  }

  private Map<String, String> getMessages() {
    return messages.get(languageTag);
  }

  private static Map<String, String> loadMessages(String languageTag) {
    final EntityManager em;

    try {
      em = JPA.em();
    } catch (Throwable e) {
      log.error("Failed to obtain entity manager", e);
      return Collections.emptyMap();
    }

    final String language = Locale.forLanguageTag(languageTag).getLanguage();
    final int limit = 1000;
    final TypedQuery<Object[]> query =
        em.createQuery(
                """
                SELECT self.key, MAX(CASE WHEN self.language = :lang THEN self.message ELSE base.message END)
                FROM MetaTranslation self
                LEFT JOIN MetaTranslation base ON base.key = self.key AND base.language = :baseLang
                WHERE self.message IS NOT NULL AND self.language IN (:lang, :baseLang)
                GROUP BY self.key
                ORDER BY self.key
                """,
                Object[].class)
            .setParameter("lang", languageTag)
            .setParameter("baseLang", language)
            .setFlushMode(FlushModeType.COMMIT)
            .setMaxResults(limit);

    int offset = 0;
    List<Object[]> results;

    Map<String, String> loadedMessages = new HashMap<>();

    do {
      query.setFirstResult(offset);
      results = query.getResultList();
      for (final Object[] result : results) {
        loadedMessages.put((String) result[0], (String) result[1]);
      }
      offset += limit;
    } while (results.size() >= limit);

    return loadedMessages;
  }

  public static void invalidate() {
    ResourceBundle.clearCache();
    messages.invalidateAll();
  }
}
