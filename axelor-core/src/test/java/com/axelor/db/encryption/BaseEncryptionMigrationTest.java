/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.encryption;

import static org.junit.jupiter.api.Assertions.*;

import com.axelor.common.crypto.BytesEncryptor;
import com.axelor.common.crypto.StringEncryptor;
import com.axelor.db.JPA;
import com.axelor.db.converters.EncryptedFieldService;
import com.axelor.db.internal.DBHelper;
import com.axelor.inject.Beans;
import com.axelor.test.db.SecureEntity;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
abstract class BaseEncryptionMigrationTest extends BaseEncryptionTest {

  protected static final int TEST_COUNT = 125;

  private static final String INSERT_SQL =
      "INSERT INTO secure_entity(id, version, my_secure_string, my_secure_binary, my_string, my_binary)"
          + " VALUES(?, 0, ?, ?, ?, ?)";

  /**
   * Inserts 2000 rows into {@code secure_entity}. Pass {@code null} encryptors to insert plain-text
   * values (initial-encryption scenario).
   */
  protected static void insertData(StringEncryptor stringEncryptor, BytesEncryptor bytesEncryptor) {
    JPA.runInTransaction(
        () ->
            JPA.jdbcWork(
                connection -> {
                  try (PreparedStatement ps = connection.prepareStatement(INSERT_SQL)) {
                    for (int i = 1; i <= TEST_COUNT; i++) {
                      String text = "EncodedMigrationTestData" + i;
                      byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
                      ps.setLong(1, i);
                      ps.setString(
                          2, stringEncryptor != null ? stringEncryptor.encrypt(text) : text);
                      ps.setBytes(
                          3, bytesEncryptor != null ? bytesEncryptor.encrypt(bytes) : bytes);
                      ps.setString(4, "MigrationTestData" + i);
                      ps.setBytes(5, ("MigrationTestData" + i).getBytes(StandardCharsets.UTF_8));
                      ps.addBatch();
                    }
                    ps.executeBatch();
                  }
                }));

    updateNextSequence(INSERT_SQL.split("(?i)INTO ")[1].split("\\(")[0].trim());
  }

  @Test
  @Order(1)
  public void runMigration() {
    System.setProperty("axelor.task.database", "encrypt");
    try {
      Beans.get(EncryptedFieldService.class).migrate(SecureEntity.class);
    } finally {
      System.clearProperty("axelor.task.database");
    }
  }

  @Test
  @Order(2)
  @SuppressWarnings("rawtypes")
  public void verifyDataMigrated() {
    inTransaction(
        () -> {
          JPA.em().clear();
          for (int i = 1; i <= 10; i++) {
            // Check if it can decode

            SecureEntity entity = JPA.em().find(SecureEntity.class, (long) i);

            assertEquals("MigrationTestData" + i, entity.getMyString());
            assertEquals("EncodedMigrationTestData" + i, entity.getMySecureString());

            assertArrayEquals(
                ("MigrationTestData" + i).getBytes(StandardCharsets.UTF_8), entity.getMyBinary());
            assertArrayEquals(
                ("EncodedMigrationTestData" + i).getBytes(StandardCharsets.UTF_8),
                entity.getMySecureBinary());

            // Check if encoded in database

            List result =
                JPA.em()
                    .createNativeQuery(
                        "SELECT my_string, my_secure_string, my_binary, my_secure_binary"
                            + " FROM secure_entity WHERE id = "
                            + i)
                    .getResultList();

            Object[] data = (Object[]) result.getFirst();
            assertTrue(getStringEncryptor().isEncrypted(data[1].toString()));
            assertEquals(
                "EncodedMigrationTestData" + i, getStringEncryptor().decrypt(data[1].toString()));
            assertTrue(getBytesEncryptor().isEncrypted((byte[]) data[3]));
            assertArrayEquals(
                ("EncodedMigrationTestData" + i).getBytes(StandardCharsets.UTF_8),
                getBytesEncryptor().decrypt((byte[]) data[3]));
          }
        });
  }

  /// /////////////
  /// Update sequence in database due to manual insert
  /// /////////////

  protected static Long findCurrentId(String tableName) {
    final long[] id = {0};
    JPA.runInTransaction(
        () ->
            JPA.jdbcWork(
                connection -> {
                  // find the max id on the table
                  String query = "SELECT MAX(id) FROM " + tableName;
                  try (PreparedStatement ps = connection.prepareStatement(query)) {
                    try (ResultSet rs = ps.executeQuery()) {
                      if (rs.next()) {
                        id[0] = rs.getLong(1);
                      }
                    }
                  }
                }));
    return id[0];
  }

  protected static void updateNextSequence(String tableName) {
    if (!DBHelper.isPostgreSQL()) {
      return;
    }
    AtomicReference<Long> currentId = new AtomicReference<>(findCurrentId(tableName));
    String sequenceName = tableName + "_seq";

    JPA.runInTransaction(
        () ->
            JPA.jdbcWork(
                connection -> {
                  // find the max id on the table
                  String query = "SELECT setval('" + sequenceName + "', " + currentId + ")";
                  try (PreparedStatement ps = connection.prepareStatement(query)) {
                    ps.executeQuery();
                  }
                }));
  }
}
