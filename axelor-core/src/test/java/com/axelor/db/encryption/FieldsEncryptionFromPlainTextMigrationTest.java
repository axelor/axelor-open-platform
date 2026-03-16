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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.converters.EncryptedFieldService;
import com.axelor.db.mapper.Mapper;
import com.axelor.inject.Beans;
import com.axelor.test.GuiceModules;
import com.axelor.test.db.StrategyClass;
import com.axelor.test.db.StrategyClassChild;
import com.axelor.test.db.StrategyJoined;
import com.axelor.test.db.StrategyJoinedChild;
import com.axelor.test.db.StrategySingle;
import com.axelor.test.db.StrategySingleChild;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Verifies the initial-encryption migration scenario: fields that were stored as plain text (before
 * any encryption was configured) are correctly encrypted during migration. Simply encrypts
 * previously unencrypted values.
 */
@GuiceModules(BaseEncryptionTest.AuditTestModule.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FieldsEncryptionFromPlainTextMigrationTest extends BaseEncryptionMigrationTest {

  @BeforeAll
  public static void setup() {
    insertData(null, null);
    insertDataForInheritanceEntities();
  }

  static List<Class<? extends Model>> entities =
      List.of(
          StrategyJoined.class,
          StrategyJoinedChild.class,
          StrategyClass.class,
          StrategyClassChild.class,
          StrategySingle.class,
          StrategySingleChild.class);

  @Test
  @Order(1)
  public void migrateInheritanceEntities() {
    System.setProperty("axelor.task.database", "encrypt");
    try {
      for (Class<? extends Model> klass : entities) {
        Beans.get(EncryptedFieldService.class).migrate(klass);
      }
    } finally {
      System.clearProperty("axelor.task.database");
    }
  }

  @Test
  @Order(2)
  public void verifyDataMigratedForInheritanceEntities() {
    for (Class<? extends Model> klass : entities) {
      Model bean = JPA.all(klass).fetchOne();
      Mapper mapper = Mapper.of(klass);

      assertEquals("MigrationTestData", mapper.get(bean, "myString").toString());
      assertEquals("EncodedMigrationTestData", mapper.get(bean, "mySecureString").toString());

      if (mapper.getProperty("myStringChild") != null) {
        assertEquals("MigrationTestDataChild", mapper.get(bean, "myStringChild").toString());
      }

      if (mapper.getProperty("mySecureStringChild") != null) {
        assertEquals(
            "EncodedMigrationTestDataChild", mapper.get(bean, "mySecureStringChild").toString());
      }
    }
  }

  /// /////////////
  /// Load data for inheritance entities
  /// /////////////

  private static void insertDataForInheritanceEntities() {
    insertForSingle();
    insertForClass();
    insertForJoined();
  }

  protected static void insertDataForInheritanceEntities(String sql, List<Object> values) {
    String tablename = sql.split("(?i)INTO ")[1].split("\\(")[0].trim();
    if (tablename.equalsIgnoreCase("strategy_class_child")) {
      // it use the sequence of the parent table
      tablename = "strategy_class";
    }
    AtomicReference<Long> currentId = new AtomicReference<>(findCurrentId(tablename));

    JPA.runInTransaction(
        () ->
            JPA.jdbcWork(
                connection -> {
                  try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    for (int i = 1; i <= 100; i++) {
                      ps.setLong(1, currentId.getAndSet(currentId.get() + 1) + 1);
                      for (int j = 1; j <= values.size(); j++) {
                        if (values.get(j - 1) instanceof Integer) {
                          ps.setInt(j + 1, Integer.valueOf((String) values.get(j - 1)));
                        } else {
                          ps.setString(j + 1, values.get(j - 1).toString());
                        }
                      }
                      ps.addBatch();
                    }
                    ps.executeBatch();
                  }
                }));

    updateNextSequence(tablename);
  }

  private static void insertForJoined() {
    String insertJoinded =
        "INSERT INTO strategy_joined(id, version, my_string, my_secure_string)"
            + " VALUES(?, 0, ?, ?)";

    insertDataForInheritanceEntities(
        insertJoinded, List.of("MigrationTestData", "EncodedMigrationTestData"));

    // First insert child elements in strategy_joined then add them in strategy_joined_child
    String insertJoinded2 =
        "INSERT INTO strategy_joined(id, version, my_string, my_secure_string)"
            + " VALUES(?, 0, ?, ?)";

    insertDataForInheritanceEntities(
        insertJoinded2, List.of("MigrationTestData", "EncodedMigrationTestData"));

    String insertJoinedChildInJoinedTable =
        "INSERT INTO strategy_joined_child(id, my_string_child, my_secure_string_child)"
            + " VALUES(?, ?, ?)";

    JPA.runInTransaction(
        () ->
            JPA.jdbcWork(
                connection -> {
                  try (PreparedStatement ps =
                      connection.prepareStatement(insertJoinedChildInJoinedTable)) {
                    for (int i = 101; i <= 200; i++) {
                      ps.setLong(1, (long) i);
                      ps.setString(2, "MigrationTestDataChild");
                      ps.setString(3, "EncodedMigrationTestDataChild");
                      ps.addBatch();
                    }
                    ps.executeBatch();
                  }
                }));
  }

  private static void insertForClass() {
    String insertClass =
        "INSERT INTO strategy_class(id, version, my_string, my_secure_string)"
            + " VALUES(?, 0, ?, ?)";

    insertDataForInheritanceEntities(
        insertClass, List.of("MigrationTestData", "EncodedMigrationTestData"));

    String insertClassChild =
        "INSERT INTO strategy_class_child(id, version, my_string, my_secure_string, my_string_child, my_secure_string_child)"
            + " VALUES(?, 0, ?, ?, ?, ?)";

    insertDataForInheritanceEntities(
        insertClassChild,
        List.of(
            "MigrationTestData",
            "EncodedMigrationTestData",
            "MigrationTestDataChild",
            "EncodedMigrationTestDataChild"));
  }

  private static void insertForSingle() {
    String insertSingle =
        "INSERT INTO strategy_single(id, version, my_string, my_secure_string, dtype)"
            + " VALUES(?, 0, ?, ?, ?)";

    insertDataForInheritanceEntities(
        insertSingle, List.of("MigrationTestData", "EncodedMigrationTestData", "StrategySingle"));

    String insertSingleChild =
        "INSERT INTO strategy_single(id, version, my_string, my_secure_string, my_string_child, my_secure_string_child, dtype)"
            + " VALUES(?, 0, ?, ?, ?, ?, ?)";

    insertDataForInheritanceEntities(
        insertSingleChild,
        List.of(
            "MigrationTestData",
            "EncodedMigrationTestData",
            "MigrationTestDataChild",
            "EncodedMigrationTestDataChild",
            "StrategySingleChild"));
  }
}
