/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.i18n;

import com.axelor.cache.AxelorCache;
import com.axelor.cache.AxelorTopic;
import com.axelor.cache.CacheBuilder;
import com.axelor.cache.DistributedFactory;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.TypedQuery;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The database backed {@link ResourceBundle} that loads translations from the axelor database. */
public class I18nBundle extends ResourceBundle {

  private final String languageTag;

  private static final AxelorCache<String, Map<String, String>> messages =
      CacheBuilder.newBuilder("messages").build(I18nBundle::loadMessages);
  private static final AxelorCache<String, String> hashes =
      CacheBuilder.newBuilder("hashes").build(I18nBundle::computeHash);

  /** Local cache in front of the possibly distributed {@link #messages} cache */
  private static final AxelorCache<String, Map<String, String>> localMessages =
      CacheBuilder.newInMemoryBuilder()
          .expireAfterWrite(Duration.ofMinutes(10))
          .build(messages::get);

  /** Topic used to notify other instances to invalidate their local caches. */
  private static final AxelorTopic invalidationTopic = DistributedFactory.getTopic("invalidation");

  static {
    invalidationTopic.addListener(String.class, msg -> invalidateLocal());
  }

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
    return getMessages(languageTag);
  }

  private static Map<String, String> getMessages(String languageTag) {
    return Objects.requireNonNullElse(localMessages.get(languageTag), Collections.emptyMap());
  }

  private static Map<String, String> loadMessages(String languageTag) {
    try {
      final EntityManager em = JPA.em();

      final String language = Locale.forLanguageTag(languageTag).getLanguage();
      final int limit = 1000;
      final TypedQuery<String[]> query =
          em.createQuery(
                  """
                  SELECT self.key, MAX(CASE WHEN self.language = :lang THEN self.message ELSE base.message END)
                  FROM MetaTranslation self
                  LEFT JOIN MetaTranslation base ON base.key = self.key AND base.language = :baseLang
                  WHERE self.message IS NOT NULL AND self.language IN (:lang, :baseLang)
                  GROUP BY self.key
                  ORDER BY self.key
                  """,
                  String[].class)
              .setParameter("lang", languageTag)
              .setParameter("baseLang", language)
              .setFlushMode(FlushModeType.COMMIT)
              .setMaxResults(limit);

      int offset = 0;
      List<String[]> results;

      Map<String, String> loadedMessages = new HashMap<>();

      do {
        query.setFirstResult(offset);
        results = query.getResultList();
        for (final String[] result : results) {
          loadedMessages.put(result[0], result[1]);
        }
        offset += limit;
      } while (results.size() >= limit);

      return loadedMessages;
    } catch (Throwable e) {
      // Can fail if JPA is not yet available or in case of temporarily unavailable database.
      // Return null to avoid caching the failure, so that it can be retried on the next access.
      log.error("Failed to load translations for language: {}", languageTag, e);
      return null;
    }
  }

  private static String computeHash(String languageTag) {
    MessageDigest messageDigest;
    try {
      messageDigest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    var msgs = getMessages(languageTag);

    if (msgs.isEmpty()) {
      return null;
    }

    msgs.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            entry -> {
              messageDigest.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
              messageDigest.update(entry.getValue().getBytes(StandardCharsets.UTF_8));
            });

    return HexFormat.of().formatHex(messageDigest.digest());
  }

  /**
   * Returns the messages hash for the given locale.
   *
   * @param locale the locale
   * @return the messages hash
   */
  public static String getHash(Locale locale) {
    return Objects.requireNonNullElse(hashes.get(locale.toLanguageTag()), "");
  }

  public static void invalidate() {
    messages.invalidateAll();
    hashes.invalidateAll();
    invalidateLocal();

    // Notify all instances to invalidate their local caches.
    invalidationTopic.publish("invalidate");
  }

  /** Invalidates local caches in response to a translation invalidation. */
  private static void invalidateLocal() {
    ResourceBundle.clearCache();
    localMessages.invalidateAll();
  }
}
