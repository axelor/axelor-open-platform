/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.encryption;

import static org.junit.jupiter.api.Assertions.*;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.test.db.SecureEntity;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

/** Verifies that JPA field encryption works correctly for normal read/write operations: */
public class FieldsEncryptionTest extends BaseEncryptionTest {

  private static Long savedEntityId = 0L;

  @BeforeAll
  public static void beforeAll() {
    if (Query.of(SecureEntity.class).count() == 0) {
      JPA.runInTransaction(
          () -> {
            SecureEntity entity = new SecureEntity();
            entity.setMyString("PlainText");
            entity.setMySecureString("EncodedText");
            entity.setMyBinary(("PlainByte").getBytes(StandardCharsets.UTF_8));
            entity.setMySecureBinary(("EncodedByte").getBytes(StandardCharsets.UTF_8));
            JPA.persist(entity);
            savedEntityId = entity.getId();
          });
    }
  }

  @SuppressWarnings("rawtypes")
  @Test
  @Order(1)
  public void dataInDatabaseShouldBeEncoded() {
    List result =
        JPA.em()
            .createNativeQuery(
                "SELECT my_string, my_secure_string, my_binary, my_secure_binary"
                    + " FROM secure_entity WHERE id = "
                    + savedEntityId)
            .getResultList();

    Object[] data = (Object[]) result.getFirst();

    assertEquals("PlainText", String.valueOf(data[0]));
    assertTrue(getStringEncryptor().isEncrypted(data[1].toString()));
    assertEquals("EncodedText", getStringEncryptor().decrypt(data[1].toString()));

    assertEquals("PlainByte", new String((byte[]) data[2], StandardCharsets.UTF_8));
    assertTrue(getBytesEncryptor().isEncrypted((byte[]) data[3]));
    assertArrayEquals(
        ("EncodedByte").getBytes(StandardCharsets.UTF_8),
        getBytesEncryptor().decrypt((byte[]) data[3]));
  }

  @Test
  @Order(2)
  public void nullEncryptedFieldsShouldStayNull() {
    final long[] savedId = {0L};

    inTransaction(
        () -> {
          SecureEntity entity = new SecureEntity();
          entity.setMySecureString(null);
          entity.setMySecureBinary(null);
          JPA.save(entity);
          savedId[0] = entity.getId();
        });

    inTransaction(
        () -> {
          SecureEntity entity = JPA.em().find(SecureEntity.class, savedId[0]);
          assertNotNull(entity);
          assertNull(entity.getMySecureString());
          assertNull(entity.getMySecureBinary());
        });
  }

  @Test
  @Order(3)
  public void reEncryptedFieldsShouldNotBeDoubleEncrypted() {
    final long[] savedId = {0L};
    final String plainText = "ToBeReEncrypted";
    final byte[] plainBytes = plainText.getBytes(StandardCharsets.UTF_8);

    inTransaction(
        () -> {
          SecureEntity entity = new SecureEntity();
          entity.setMySecureString(plainText);
          entity.setMySecureBinary(plainBytes);
          JPA.save(entity);
          savedId[0] = entity.getId();
        });

    // Read and re-save — simulates an update cycle
    inTransaction(
        () -> {
          SecureEntity entity = JPA.em().find(SecureEntity.class, savedId[0]);
          entity.setMyString("Hello");
          JPA.save(entity);
        });

    // Values should still be the original plain text, not double-encrypted
    inTransaction(
        () -> {
          SecureEntity entity = JPA.em().find(SecureEntity.class, savedId[0]);
          assertEquals(plainText, entity.getMySecureString());
          assertArrayEquals(plainBytes, entity.getMySecureBinary());
        });
  }

  @Test
  @Order(4)
  public void shouldDecodeEncodedData() {
    SecureEntity entity = JPA.em().find(SecureEntity.class, savedEntityId);

    assertEquals("PlainText", entity.getMyString());
    assertEquals("EncodedText", entity.getMySecureString());
    assertArrayEquals("PlainByte".getBytes(StandardCharsets.UTF_8), entity.getMyBinary());
    assertArrayEquals("EncodedByte".getBytes(StandardCharsets.UTF_8), entity.getMySecureBinary());
  }
}
