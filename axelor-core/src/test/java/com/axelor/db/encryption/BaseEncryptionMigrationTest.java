/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2026 Axelor (<http://axelor.com>).
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

  private static String getInsertSql() {
    String blobExpr = DBHelper.isPostgreSQL() ? "lo_from_bytea(0, ?)" : "?";
    return String.format(
        "INSERT INTO secure_entity(id, version, my_secure_string, my_secure_binary, my_string,"
            + " my_binary) VALUES(?, 0, ?, %s, ?, %s)",
        blobExpr, blobExpr);
  }

  /**
   * Inserts 2000 rows into {@code secure_entity}. Pass {@code null} encryptors to insert plain-text
   * values (initial-encryption scenario).
   */
  protected static void insertData(StringEncryptor stringEncryptor, BytesEncryptor bytesEncryptor) {
    String insertSql = getInsertSql();
    JPA.runInTransaction(
        () ->
            JPA.jdbcWork(
                connection -> {
                  try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    for (int i = 1; i <= 2000; i++) {
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

    updateNextSequence(insertSql.split("(?i)INTO ")[1].split("\\(")[0].trim());
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
                    .unwrap(org.hibernate.query.NativeQuery.class)
                    .addScalar("my_string", org.hibernate.type.StandardBasicTypes.STRING)
                    .addScalar("my_secure_string", org.hibernate.type.StandardBasicTypes.STRING)
                    // MATERIALIZED_BLOB tells Hibernate to resolve the OID to actual bytes
                    .addScalar("my_binary", org.hibernate.type.StandardBasicTypes.MATERIALIZED_BLOB)
                    .addScalar(
                        "my_secure_binary", org.hibernate.type.StandardBasicTypes.MATERIALIZED_BLOB)
                    .getResultList();

            Object[] data = (Object[]) result.get(0);
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
