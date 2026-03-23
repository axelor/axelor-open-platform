/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.converters;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.StringUtils;
import com.axelor.common.crypto.BytesEncryptorPbkdf2Sha256;
import com.axelor.common.crypto.Encryptor;
import com.axelor.common.crypto.OperationMode;
import com.axelor.common.crypto.StringEncryptorPbkdf2Sha256;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.db.mapper.PropertyType;
import com.google.inject.persist.Transactional;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.TypedQuery;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
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

  private boolean settingsValidated = false;

  /**
   * Iteration count used when re-encrypting during migration.
   *
   * <p><strong>Conscious security trade-off:</strong> 100K iterations is intentionally below the
   * OWASP 2023 recommendation of 600,000 for PBKDF2-SHA256, chosen to keep migration throughput
   * acceptable on large datasets.
   */
  private int defaultIteration = 100_000;

  /**
   * Overrides the default iteration count used when re-encrypting during migration.
   *
   * @param iteration the number of hash iterations to use
   */
  public void setIteration(int iteration) {
    this.defaultIteration = iteration;
  }

  /**
   * Migrates all encrypted field values across all models.
   *
   * <p>Iterates over every registered model and re-encrypts each encrypted field using the new
   * algorithm and password configured in <code>axelor-config.properties</code>.
   *
   * @throws IllegalStateException if the new encryption password is not configured
   */
  @Transactional
  public void migrate() {
    validateAndLogMigrationSettings();

    long startTime = System.currentTimeMillis();
    JPA.models().forEach(this::migrate);
    LOG.info(
        "Encrypted fields migration completed in {}",
        formatDuration(System.currentTimeMillis() - startTime));
  }

  private static final List<String> KNOWN_ALGORITHMS = List.of("GCM", "CBC");

  private void validateAndLogMigrationSettings() {
    if (settingsValidated) {
      return;
    }

    if (StringUtils.isBlank(ENCRYPTION_PASSWORD)) {
      throw new IllegalStateException("Encryption password is required for migration");
    }

    String newAlgorithm = ENCRYPTION_ALGORITHM;
    String oldAlgorithm = AppSettings.get().get(AvailableAppSettings.ENCRYPTION_OLD_ALGORITHM);
    String oldPassword = AppSettings.get().get(AvailableAppSettings.ENCRYPTION_OLD_PASSWORD);

    if (StringUtils.notBlank(newAlgorithm)
        && KNOWN_ALGORITHMS.stream().noneMatch(newAlgorithm::equalsIgnoreCase)) {
      throw new IllegalStateException(
          "Unknown encryption algorithm '%s'. Supported values: %s"
              .formatted(newAlgorithm, KNOWN_ALGORITHMS));
    }

    if (StringUtils.notBlank(oldAlgorithm)
        && KNOWN_ALGORITHMS.stream().noneMatch(oldAlgorithm::equalsIgnoreCase)) {
      throw new IllegalStateException(
          "Unknown old encryption algorithm '%s'. Supported values: %s"
              .formatted(oldAlgorithm, KNOWN_ALGORITHMS));
    }

    if (StringUtils.notBlank(oldPassword) && oldPassword.equals(ENCRYPTION_PASSWORD)) {
      LOG.warn(
          "encryption.old-password is the same as encryption.password — password rotation will have no effect");
    } else if (StringUtils.notBlank(oldPassword)) {
      LOG.info("Encryption migration: rotating password");
    }

    if (StringUtils.notBlank(oldAlgorithm) && oldAlgorithm.equalsIgnoreCase(newAlgorithm)) {
      LOG.warn(
          "encryption.old-algorithm is the same as encryption.algorithm — algorithm change will have no effect");
    } else if (StringUtils.notBlank(oldAlgorithm)) {
      LOG.info("Encryption migration: changing algorithm");
    }

    if (StringUtils.isBlank(oldPassword) && StringUtils.isBlank(oldAlgorithm)) {
      LOG.info(
          "Encryption migration: encrypting plain-text field values or change of algorithm version");
    }

    settingsValidated = true;
  }

  /**
   * Migrates encrypted field values for the given model.
   *
   * <p>If no field names are provided, all encrypted fields of the model are migrated. Each value
   * is decrypted using the old encryptor (via the JPA converter) and re-encrypted using the new
   * algorithm and password. Updates are executed in batches via JDBC for efficiency.
   *
   * @param model the model class whose encrypted fields should be migrated
   * @param fields optional list of specific field names to migrate; if empty, all encrypted fields
   *     are migrated
   * @throws IllegalStateException if the new encryption password is not configured
   */
  @Transactional
  public void migrate(Class<?> model, String... fields) {
    validateAndLogMigrationSettings();

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

    Class<?> superclass = model.getSuperclass();
    while (superclass != null && superclass != Model.class) {
      if (JPA.models().contains(superclass)) {
        // With TABLE_PER_CLASS, each concrete table has its own physical copy of inherited
        // columns, so inherited fields must be migrated independently per class.
        Inheritance inheritance = superclass.getAnnotation(Inheritance.class);
        if (inheritance == null || inheritance.strategy() != InheritanceType.TABLE_PER_CLASS) {
          final Mapper superMapper = Mapper.of(superclass);
          encrypted.removeIf(p -> superMapper.getProperty(p.getName()) != null);
        }
        break;
      }
      superclass = superclass.getSuperclass();
    }

    if (encrypted.isEmpty()) {
      return;
    }

    boolean hasLarge =
        encrypted.stream()
            .map(Property::getType)
            .anyMatch(t -> t == PropertyType.BINARY || t == PropertyType.TEXT);

    final String selectSql =
        "SELECT new Map(self.id as id, %s) FROM %s self ORDER BY self.id"
            .formatted(
                encrypted.stream()
                    .map(n -> "self." + n.getName() + " as " + n.getName())
                    .collect(Collectors.joining(", ")),
                model.getSimpleName());

    final String updateSql =
        "UPDATE %s SET %s WHERE id = ?"
            .formatted(
                getTableName(model),
                encrypted.stream()
                    .map(n -> getColumnName(model, n.getName()) + " = ?")
                    .collect(Collectors.joining(", ")));

    TypedQuery<Map> selectQuery = JPA.em().createQuery(selectSql, Map.class);
    selectQuery.setFlushMode(FlushModeType.COMMIT);

    long count = countRecords(model);
    int offset = 0;
    int limit = hasLarge ? 100 : 1000;
    int[] updatedRecords = {0};
    long startTime = System.currentTimeMillis();

    LOG.info(
        "Migrating {} encrypted field(s) across {} record(s) in {}",
        encrypted.stream().map(Property::getName).collect(Collectors.joining(", ")),
        count,
        model.getSimpleName());

    selectQuery.setMaxResults(limit);
    while (offset < count) {
      LOG.debug("Processing records {} to {}", offset, Math.min(count, (offset + limit)));
      selectQuery.setFirstResult(offset);
      List<Map> values = selectQuery.getResultList();
      offset += limit;

      JPA.jdbcWork(
          connection -> {
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
              for (Map map : values) {
                for (int i = 0; i < encrypted.size(); i++) {
                  Property property = encrypted.get(i);
                  if (property.getJavaType() == String.class) {
                    ps.setString(
                        i + 1, getStringEncryptor().encrypt((String) map.get(property.getName())));
                  } else if (property.getJavaType() == byte[].class) {
                    ps.setBytes(
                        i + 1, getBytesEncryptor().encrypt((byte[]) map.get(property.getName())));
                  } else {
                    throw new IllegalArgumentException(
                        "Unsupported encryption type for field: " + property.getName());
                  }
                }

                ps.setLong(encrypted.size() + 1, (Long) map.get("id"));
                ps.addBatch();
              }
              int[] counts = ps.executeBatch();
              for (int c : counts) {
                if (c > 0) updatedRecords[0] += c;
              }
            }
          });
    }

    LOG.info(
        "{} record(s) migrated in {} in {}",
        updatedRecords[0],
        model.getSimpleName(),
        formatDuration(System.currentTimeMillis() - startTime));
  }

  /**
   * Formats a duration in milliseconds into a human-readable string (e.g. {@code 1m 23s 456ms}).
   *
   * @param millis the duration in milliseconds
   * @return a human-readable duration string
   */
  private static String formatDuration(long millis) {
    long minutes = millis / 60_000;
    long seconds = (millis % 60_000) / 1000;
    long ms = millis % 1000;
    if (minutes > 0) return "%dm %ds %dms".formatted(minutes, seconds, ms);
    if (seconds > 0) return "%ds %dms".formatted(seconds, ms);
    return "%dms".formatted(ms);
  }

  /**
   * Returns the total number of records for the given model.
   *
   * @param model the model class to count
   * @return the record count
   */
  private long countRecords(Class<?> model) {
    TypedQuery<Long> countQuery =
        JPA.em().createQuery("SELECT COUNT(m.id) FROM " + model.getName() + " m", Long.class);
    countQuery.setFlushMode(FlushModeType.COMMIT);
    return countQuery.getSingleResult();
  }

  /**
   * Resolves the database column name for the given field of an entity class.
   *
   * @param entityClass the entity class
   * @param fieldName the JPA field name
   * @return the mapped column name
   * @throws IllegalStateException if the persister type is not supported or the field maps to
   *     multiple columns
   * @throws IllegalArgumentException if no column is mapped for the given field
   */
  private String getColumnName(Class<?> entityClass, String fieldName) {
    SessionFactoryImplementor sfi =
        JPA.em().getEntityManagerFactory().unwrap(SessionFactoryImplementor.class);
    EntityPersister persister = sfi.getMappingMetamodel().getEntityDescriptor(entityClass);

    if (!(persister instanceof AbstractEntityPersister aep)) {
      throw new IllegalStateException(
          "Unsupported persister type for entity %s: %s"
              .formatted(entityClass.getName(), persister.getClass().getName()));
    }

    String[] columnNames = aep.getPropertyColumnNames(fieldName);

    if (columnNames == null || columnNames.length == 0) {
      throw new IllegalArgumentException(
          "No column mapped for property '%s' of entity '%s'"
              .formatted(fieldName, entityClass.getName()));
    }

    if (columnNames.length != 1) {
      throw new IllegalStateException(
          "Expected exactly one column for property '%s' of entity '%s', but got %d"
              .formatted(fieldName, entityClass.getName(), columnNames.length));
    }

    return columnNames[0];
  }

  /**
   * Resolves the database table name for the given entity class.
   *
   * @param entityClass the entity class
   * @return the mapped table name
   * @throws IllegalStateException if the persister type is not supported
   */
  private static String getTableName(Class<?> entityClass) {
    SessionFactoryImplementor sfi =
        JPA.em().getEntityManagerFactory().unwrap(SessionFactoryImplementor.class);

    EntityPersister persister = sfi.getMappingMetamodel().getEntityDescriptor(entityClass);

    if (!(persister instanceof AbstractEntityPersister aep)) {
      throw new IllegalStateException(
          "Unsupported persister type for entity %s: %s"
              .formatted(entityClass.getName(), persister.getClass().getName()));
    }

    return aep.getMappedTableDetails().getTableName();
  }

  private final String ENCRYPTION_ALGORITHM =
      AppSettings.get().get(AvailableAppSettings.ENCRYPTION_ALGORITHM);
  private final String ENCRYPTION_PASSWORD =
      AppSettings.get().get(AvailableAppSettings.ENCRYPTION_PASSWORD);

  private Encryptor<String, String> stringEncryptor;
  private Encryptor<byte[], byte[]> bytesEncryptor;

  /**
   * Returns the default {@code Encryptor<String, String>} for the new encryption settings, lazily
   * initialized.
   *
   * @return the string encryptor
   */
  private Encryptor<String, String> getStringEncryptor() {
    if (stringEncryptor == null) {
      stringEncryptor =
          "GCM".equalsIgnoreCase(ENCRYPTION_ALGORITHM)
              ? new StringEncryptorPbkdf2Sha256(
                  OperationMode.GCM, ENCRYPTION_PASSWORD, defaultIteration)
              : new StringEncryptorPbkdf2Sha256(
                  OperationMode.CBC, ENCRYPTION_PASSWORD, defaultIteration);
    }
    return stringEncryptor;
  }

  /**
   * Returns the default {@code Encryptor<byte[], byte[]>} for the new encryption settings, lazily
   * initialized.
   *
   * @return the bytes encryptor
   */
  private Encryptor<byte[], byte[]> getBytesEncryptor() {
    if (bytesEncryptor == null) {
      bytesEncryptor =
          "GCM".equalsIgnoreCase(ENCRYPTION_ALGORITHM)
              ? new BytesEncryptorPbkdf2Sha256(
                  OperationMode.GCM, ENCRYPTION_PASSWORD, defaultIteration)
              : new BytesEncryptorPbkdf2Sha256(
                  OperationMode.CBC, ENCRYPTION_PASSWORD, defaultIteration);
    }
    return bytesEncryptor;
  }
}
