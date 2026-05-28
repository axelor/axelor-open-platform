/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.Arrays;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class TestBytesEncryptorPbkdf2 {

  private static final String SECRET_KEY = "MySuperSecretKey";

  abstract static class Base {

    abstract AbstractBytesEncryptorPbkdf2 encryptor();

    abstract byte[] prefixBytes();

    private byte[] generateRandomBytes(int size) {
      byte[] value = new byte[size];
      new SecureRandom().nextBytes(value);
      return value;
    }

    @Test
    void testRoundtrip() {
      final AbstractBytesEncryptorPbkdf2 encryptor = encryptor();
      final byte[] value = generateRandomBytes(18);

      final byte[] encrypted = encryptor.encrypt(value);
      final byte[] decrypted = encryptor.decrypt(encrypted);

      assertNotNull(encrypted);
      assertFalse(Arrays.equals(encrypted, value));
      assertArrayEquals(value, decrypted);

      // must carry the expected prefix
      final byte[] prefix = new byte[prefixBytes().length];
      System.arraycopy(encrypted, 0, prefix, 0, prefix.length);
      assertArrayEquals(prefixBytes(), prefix);
    }

    @Test
    void testFreshSaltPerCall() {
      // Same plaintext encrypted twice must produce different ciphertext (per-call salt)
      final AbstractBytesEncryptorPbkdf2 encryptor = encryptor();
      final byte[] value = generateRandomBytes(18);

      final byte[] first = encryptor.encrypt(value);
      final byte[] second = encryptor.encrypt(value);

      assertFalse(Arrays.equals(first, second));
    }

    @Test
    void testIsEncrypted() {
      final AbstractBytesEncryptorPbkdf2 encryptor = encryptor();
      final byte[] value = generateRandomBytes(18);
      final byte[] encrypted = encryptor.encrypt(value);

      assertTrue(encryptor.isEncrypted(encrypted));
      assertFalse(encryptor.isEncrypted(value));
      assertFalse(encryptor.isEncrypted(null));
    }

    @Test
    void testNullPassthrough() {
      final AbstractBytesEncryptorPbkdf2 encryptor = encryptor();

      assertNull(encryptor.encrypt(null));
      assertNull(encryptor.decrypt(null));
    }

    @Test
    void testAlreadyEncryptedSkipped() {
      final AbstractBytesEncryptorPbkdf2 encryptor = encryptor();
      final byte[] encrypted = encryptor.encrypt(generateRandomBytes(18));

      // encrypting an already-encrypted payload must return it unchanged
      assertArrayEquals(encrypted, encryptor.encrypt(encrypted));
    }
  }

  @Nested
  class GcmSha256 extends Base {
    @Override
    BytesEncryptorPbkdf2Sha256 encryptor() {
      return BytesEncryptorPbkdf2Sha256.gcm(SECRET_KEY);
    }

    @Override
    byte[] prefixBytes() {
      return BytesEncryptorPbkdf2Sha256.PREFIX_BYTES;
    }
  }

  @Nested
  class CbcSha256 extends Base {
    @Override
    BytesEncryptorPbkdf2Sha256 encryptor() {
      return BytesEncryptorPbkdf2Sha256.cbc(SECRET_KEY);
    }

    @Override
    byte[] prefixBytes() {
      return BytesEncryptorPbkdf2Sha256.PREFIX_BYTES;
    }
  }

  @Nested
  class GcmSha512 extends Base {
    @Override
    BytesEncryptorPbkdf2Sha512 encryptor() {
      return BytesEncryptorPbkdf2Sha512.gcm(SECRET_KEY);
    }

    @Override
    byte[] prefixBytes() {
      return BytesEncryptorPbkdf2Sha512.PREFIX_BYTES;
    }
  }

  @Nested
  class CbcSha512 extends Base {
    @Override
    BytesEncryptorPbkdf2Sha512 encryptor() {
      return BytesEncryptorPbkdf2Sha512.cbc(SECRET_KEY);
    }

    @Override
    byte[] prefixBytes() {
      return BytesEncryptorPbkdf2Sha512.PREFIX_BYTES;
    }
  }
}
