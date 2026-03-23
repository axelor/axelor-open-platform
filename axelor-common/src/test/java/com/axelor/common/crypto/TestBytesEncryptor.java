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

@SuppressWarnings("deprecation")
public class TestBytesEncryptor {

  private static final String SECRET_KEY = "MySuperSecretKey";

  abstract static class Base {

    abstract BytesEncryptor encryptor();

    private byte[] generateRandomBytes(int size) {
      byte[] value = new byte[size];
      new SecureRandom().nextBytes(value);
      return value;
    }

    @Test
    void testRoundtrip() {
      final BytesEncryptor encryptor = encryptor();
      final byte[] value = generateRandomBytes(18);

      final byte[] encrypted = encryptor.encrypt(value);
      final byte[] decrypted = encryptor.decrypt(encrypted);

      assertNotNull(encrypted);
      assertFalse(Arrays.equals(encrypted, value));
      assertArrayEquals(value, decrypted);

      // make sure to have special prefix
      final byte[] prefix = new byte[BytesEncryptor.PREFIX_BYTES.length];
      System.arraycopy(encrypted, 0, prefix, 0, prefix.length);
      assertArrayEquals(BytesEncryptor.PREFIX_BYTES, prefix);
    }

    @Test
    void testIsEncrypted() {
      final BytesEncryptor encryptor = encryptor();
      final byte[] value = generateRandomBytes(18);
      final byte[] encrypted = encryptor.encrypt(value);

      assertTrue(encryptor.isEncrypted(encrypted));
      assertFalse(encryptor.isEncrypted(value));
      assertFalse(encryptor.isEncrypted((byte[]) null));
    }

    @Test
    void testNullPassthrough() {
      final BytesEncryptor encryptor = encryptor();

      assertNull(encryptor.encrypt((byte[]) null));
      assertNull(encryptor.decrypt((byte[]) null));
    }

    @Test
    void testAlreadyEncryptedSkipped() {
      final BytesEncryptor encryptor = encryptor();
      final byte[] encrypted = encryptor.encrypt(generateRandomBytes(18));

      assertArrayEquals(encrypted, encryptor.encrypt(encrypted));
    }
  }

  @Nested
  class GCM extends Base {
    @Override
    BytesEncryptor encryptor() {
      return BytesEncryptor.gcm(SECRET_KEY);
    }
  }

  @Nested
  class CBC extends Base {
    @Override
    BytesEncryptor encryptor() {
      return BytesEncryptor.cbc(SECRET_KEY);
    }
  }
}
