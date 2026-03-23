/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation")
public class TestStringEncryptor {

  private static final String SECRET_KEY = "MySuperSecretKey";

  abstract static class Base {

    abstract StringEncryptor encryptor();

    @Test
    void testRoundtrip() {
      final StringEncryptor encryptor = encryptor();
      final String value = "Hello World!!!";

      final String encrypted = encryptor.encrypt(value);
      final String decrypted = encryptor.decrypt(encrypted);

      assertNotNull(encrypted);
      assertNotEquals(value, encrypted);
      assertEquals(value, decrypted);
    }

    @Test
    void testIsEncrypted() {
      final StringEncryptor encryptor = encryptor();
      final String value = "Hello World!!!";
      final String encrypted = encryptor.encrypt(value);

      assertTrue(encryptor.isEncrypted(encrypted));
      assertFalse(encryptor.isEncrypted(value));
      assertFalse(encryptor.isEncrypted((String) null));
    }

    @Test
    void testNullPassthrough() {
      final StringEncryptor encryptor = encryptor();

      assertNull(encryptor.encrypt((String) null));
      assertNull(encryptor.decrypt((String) null));
    }

    @Test
    void testAlreadyEncryptedSkipped() {
      final StringEncryptor encryptor = encryptor();
      final String encrypted = encryptor.encrypt("Hello World!!!");

      assertEquals(encrypted, encryptor.encrypt(encrypted));
    }
  }

  @Nested
  class GCM extends Base {
    @Override
    StringEncryptor encryptor() {
      return StringEncryptor.gcm(SECRET_KEY);
    }
  }

  @Nested
  class CBC extends Base {
    @Override
    StringEncryptor encryptor() {
      return StringEncryptor.cbc(SECRET_KEY);
    }
  }
}
