/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.converters;

import com.axelor.db.JPA;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.db.mapper.PropertyType;
import com.google.inject.persist.Transactional;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The service can be used to migrate all the encrypted field values.
 *
 * <p>This utility method {@link #migrate()} can be used to migrate encrypted field values if you
 * want to change encryption algorithm or password or salt.
 *
 * <p>Following encryption settings will be read from <code>axelor-config.properties</code> for the
 * migration.
 *
 * <ul>
 *   <li><code>encryption.old-algorithm</code> the old algorithm, empty if not set previously
 *   <li><code>encryption.old-password</code> the old password, empty if not set previously
 * </ul>
 *
 * and new settings:
 *
 * <ul>
 *   <li><code>encryption.algorithm</code> the new algorithm, empty if want to use default
 *   <li><code>encryption.password</code> the new password (required)
 * </ul>
 */
public class EncryptedFieldService {

  private static final Logger LOG = LoggerFactory.getLogger(EncryptedFieldService.class);

  @Transactional
  public void migrate() {
    JPA.models().forEach(this::migrate);
  }

  @SuppressWarnings("all")
  @Transactional
  public void migrate(Class<?> model, String... fields) {
    final Mapper mapper = Mapper.of(model);
    final List<Property> encrypted = new ArrayList<>();

    if (fields == null || fields.length == 0) {
      Arrays.stream(mapper.getProperties()).filter(Property::isEncrypted).forEach(encrypted::add);
    } else {
      Arrays.stream(fields)
          .map(mapper::getProperty)
          .filter(Property::isEncrypted)
          .forEach(encrypted::add);
    }

    if (encrypted.isEmpty()) {
      return;
    }

    boolean hasLarge =
        encrypted.stream()
            .map(Property::getType)
            .anyMatch(t -> t == PropertyType.BINARY || t == PropertyType.TEXT);

    List<String> names = encrypted.stream().map(Property::getName).collect(Collectors.toList());

    final String selectSql =
        new StringBuilder("SELECT ")
            .append("new Map(self.id as id,")
            .append(
                names.stream().map(n -> "self." + n + " as " + n).collect(Collectors.joining(", ")))
            .append(") FROM ")
            .append(model.getSimpleName())
            .append(" self ORDER BY self.id")
            .toString();

    final String updateSql =
        "UPDATE "
            + model.getSimpleName()
            + " self SET "
            + names.stream().map(n -> "self." + n + " = :" + n).collect(Collectors.joining(", "))
            + " WHERE self.id = :id";

    TypedQuery<Map> selectQuery = JPA.em().createQuery(selectSql, Map.class);
    selectQuery.setFlushMode(FlushModeType.COMMIT);

    long count = countRecords(model);
    int offset = 0;
    int limit = hasLarge ? 40 : 1000;
    int updatedRecords = 0;

    LOG.info(
        "Migrating {} encrypted field(s) across {} record(s) in {}",
        names.size(),
        count,
        model.getSimpleName());

    selectQuery.setMaxResults(limit);
    while (offset < count) {
      selectQuery.setFirstResult(offset);
      List<Map> values = selectQuery.getResultList();
      LOG.debug("Processing records {} to {}", offset, Math.min(count, (offset + limit)));
      offset += limit;
      updatedRecords += values.stream().mapToInt(map -> migrateRecord(updateSql, names, map)).sum();
    }

    LOG.info("{} record(s) migrated in {}", updatedRecords, model.getSimpleName());
  }

  @SuppressWarnings("all")
  private int migrateRecord(String updateSql, List<String> names, Map map) {
    if (names.stream().allMatch(name -> map.get(name) == null)) {
      return 0;
    }
    final jakarta.persistence.Query uq = JPA.em().createQuery(updateSql);
    uq.setFlushMode(FlushModeType.COMMIT);
    uq.setParameter("id", (Long) map.get("id"));
    names.forEach(name -> uq.setParameter(name, map.get(name)));
    return uq.executeUpdate();
  }

  private long countRecords(Class<?> model) {
    TypedQuery<Long> countQuery =
        JPA.em().createQuery("SELECT COUNT(m.id) FROM " + model.getName() + " m", Long.class);
    countQuery.setFlushMode(FlushModeType.COMMIT);
    return countQuery.getSingleResult();
  }
}
