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

public class TestStringEncryptorPbkdf2 {

  private static final String SECRET_KEY = "MySuperSecretKey";

  abstract static class Base {

    abstract AbstractStringEncryptorPbkdf2 encryptor();

    @Test
    void testRoundtrip() {
      final AbstractStringEncryptorPbkdf2 encryptor = encryptor();
      final String value = "Hello World!!!";

      final String encrypted = encryptor.encrypt(value);
      final String decrypted = encryptor.decrypt(encrypted);

      assertNotNull(encrypted);
      assertNotEquals(value, encrypted);
      assertEquals(value, decrypted);
      assertTrue(encryptor.isEncrypted(encrypted));
      assertFalse(encryptor.isEncrypted(value));
    }

    @Test
    void testIsEncrypted() {
      final AbstractStringEncryptorPbkdf2 encryptor = encryptor();
      final String value = "Hello World!!!";
      final String encrypted = encryptor.encrypt(value);

      assertTrue(encryptor.isEncrypted(encrypted));
      assertFalse(encryptor.isEncrypted(value));
      assertFalse(encryptor.isEncrypted(null));
    }

    @Test
    void testNullPassthrough() {
      final AbstractStringEncryptorPbkdf2 encryptor = encryptor();

      assertNull(encryptor.encrypt(null));
      assertNull(encryptor.decrypt(null));
    }

    @Test
    void testAlreadyEncryptedSkipped() {
      final AbstractStringEncryptorPbkdf2 encryptor = encryptor();
      final String encrypted = encryptor.encrypt("Hello World!!!");

      assertEquals(encrypted, encryptor.encrypt(encrypted));
    }
  }

  @Nested
  class GcmSha256 extends Base {
    @Override
    StringEncryptorPbkdf2Sha256 encryptor() {
      return StringEncryptorPbkdf2Sha256.gcm(SECRET_KEY);
    }
  }

  @Nested
  class CbcSha256 extends Base {
    @Override
    StringEncryptorPbkdf2Sha256 encryptor() {
      return StringEncryptorPbkdf2Sha256.cbc(SECRET_KEY);
    }
  }

  @Nested
  class GcmSha512 extends Base {
    @Override
    StringEncryptorPbkdf2Sha512 encryptor() {
      return StringEncryptorPbkdf2Sha512.gcm(SECRET_KEY);
    }
  }

  @Nested
  class CbcSha512 extends Base {
    @Override
    StringEncryptorPbkdf2Sha512 encryptor() {
      return StringEncryptorPbkdf2Sha512.cbc(SECRET_KEY);
    }
  }
}
