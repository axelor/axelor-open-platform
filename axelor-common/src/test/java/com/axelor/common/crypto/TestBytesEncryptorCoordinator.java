/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class TestBytesEncryptorCoordinator {

  private static final String SECRET_KEY = "MySuperSecretKey";

  abstract static class Base {

    abstract BytesEncryptorCoordinator coordinator();

    abstract BytesEncryptorPbkdf2Sha256 v1Encryptor();

    @SuppressWarnings("deprecation")
    abstract BytesEncryptor legacyEncryptor();

    private byte[] generateRandomBytes(int size) {
      byte[] value = new byte[size];
      new SecureRandom().nextBytes(value);
      return value;
    }

    @Test
    void testEncryptsWithV1() {
      final byte[] encrypted = coordinator().encrypt(generateRandomBytes(18));

      // new encryptions must always produce the $AESv1$ format
      final byte[] prefix = new byte[BytesEncryptorPbkdf2Sha256.PREFIX_BYTES.length];
      System.arraycopy(encrypted, 0, prefix, 0, prefix.length);
      assertArrayEquals(BytesEncryptorPbkdf2Sha256.PREFIX_BYTES, prefix);
    }

    @Test
    @SuppressWarnings("deprecation")
    void testDecryptsLegacy() {
      final byte[] value = generateRandomBytes(18);
      final byte[] legacyEncrypted = legacyEncryptor().encrypt(value);

      final BytesEncryptorCoordinator coordinator = coordinator();

      assertTrue(coordinator.isEncrypted(legacyEncrypted));
      assertArrayEquals(value, coordinator.decrypt(legacyEncrypted));
    }

    @Test
    void testDecryptsV1() {
      final byte[] value = generateRandomBytes(18);
      final byte[] v1Encrypted = v1Encryptor().encrypt(value);

      final BytesEncryptorCoordinator coordinator = coordinator();

      assertTrue(coordinator.isEncrypted(v1Encrypted));
      assertArrayEquals(value, coordinator.decrypt(v1Encrypted));
    }

    @Test
    void testIsEncryptedBothFormats() {
      final BytesEncryptorCoordinator coordinator = coordinator();
      final byte[] value = generateRandomBytes(18);

      assertTrue(coordinator.isEncrypted(legacyEncryptor().encrypt(value)));
      assertTrue(coordinator.isEncrypted(v1Encryptor().encrypt(value)));
      assertFalse(coordinator.isEncrypted(value));
      assertFalse(coordinator.isEncrypted((byte[]) null));
    }

    @Test
    @SuppressWarnings("deprecation")
    void testPrefixIsolation() {
      final byte[] value = generateRandomBytes(18);
      final byte[] legacyEncrypted = BytesEncryptor.gcm(SECRET_KEY).encrypt(value);
      final byte[] v1Encrypted = v1Encryptor().encrypt(value);

      // each encryptor must only recognise its own format
      assertFalse(new BytesEncryptorPbkdf2Sha256(SECRET_KEY).isEncrypted(legacyEncrypted));
      assertFalse(new BytesEncryptor(SECRET_KEY).isEncrypted(v1Encrypted));
    }
  }

  @Nested
  @SuppressWarnings("deprecation")
  class GCM extends Base {
    @Override
    BytesEncryptorCoordinator coordinator() {
      return BytesEncryptorCoordinator.gcm(SECRET_KEY);
    }

    @Override
    BytesEncryptorPbkdf2Sha256 v1Encryptor() {
      return BytesEncryptorPbkdf2Sha256.gcm(SECRET_KEY);
    }

    @Override
    BytesEncryptor legacyEncryptor() {
      return BytesEncryptor.gcm(SECRET_KEY);
    }
  }

  @Nested
  @SuppressWarnings("deprecation")
  class CBC extends Base {
    @Override
    BytesEncryptorCoordinator coordinator() {
      return BytesEncryptorCoordinator.cbc(SECRET_KEY);
    }

    @Override
    BytesEncryptorPbkdf2Sha256 v1Encryptor() {
      return BytesEncryptorPbkdf2Sha256.cbc(SECRET_KEY);
    }

    @Override
    BytesEncryptor legacyEncryptor() {
      return BytesEncryptor.cbc(SECRET_KEY);
    }
  }
}
