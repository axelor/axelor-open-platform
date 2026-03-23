/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class TestStringEncryptorCoordinator {

  private static final String SECRET_KEY = "MySuperSecretKey";

  abstract static class Base {

    abstract StringEncryptorCoordinator coordinator();

    abstract StringEncryptorPbkdf2Sha256 v1Encryptor();

    @SuppressWarnings("deprecation")
    abstract StringEncryptor legacyEncryptor();

    @Test
    void testEncryptsWithV1() {
      final String encrypted = coordinator().encrypt("Hello World!!!");

      assertTrue(new StringEncryptorPbkdf2Sha256(SECRET_KEY).isEncrypted(encrypted));
    }

    @Test
    @SuppressWarnings("deprecation")
    void testDecryptsLegacy() {
      final String value = "Hello World!!!";
      final String legacyEncrypted = legacyEncryptor().encrypt(value);

      final StringEncryptorCoordinator coordinator = coordinator();

      assertTrue(coordinator.isEncrypted(legacyEncrypted));
      assertEquals(value, coordinator.decrypt(legacyEncrypted));
    }

    @Test
    void testDecryptsV1() {
      final String value = "Hello World!!!";
      final String v1Encrypted = v1Encryptor().encrypt(value);

      final StringEncryptorCoordinator coordinator = coordinator();

      assertTrue(coordinator.isEncrypted(v1Encrypted));
      assertEquals(value, coordinator.decrypt(v1Encrypted));
    }

    @Test
    void testIsEncryptedBothFormats() {
      final StringEncryptorCoordinator coordinator = coordinator();
      final String value = "Hello World!!!";

      assertTrue(coordinator.isEncrypted(legacyEncryptor().encrypt(value)));
      assertTrue(coordinator.isEncrypted(v1Encryptor().encrypt(value)));
      assertFalse(coordinator.isEncrypted(value));
      assertFalse(coordinator.isEncrypted((String) null));
    }

    @Test
    @SuppressWarnings("deprecation")
    void testPrefixIsolation() {
      final String value = "Hello World!!!";
      final String legacyEncrypted = legacyEncryptor().encrypt(value);
      final String v1Encrypted = v1Encryptor().encrypt(value);

      // each encryptor must only recognise its own format
      assertFalse(new StringEncryptorPbkdf2Sha256(SECRET_KEY).isEncrypted(legacyEncrypted));
      assertFalse(new StringEncryptor(new BytesEncryptor(SECRET_KEY)).isEncrypted(v1Encrypted));
    }
  }

  @Nested
  @SuppressWarnings("deprecation")
  class GCM extends Base {
    @Override
    StringEncryptorCoordinator coordinator() {
      return StringEncryptorCoordinator.gcm(SECRET_KEY);
    }

    @Override
    StringEncryptorPbkdf2Sha256 v1Encryptor() {
      return StringEncryptorPbkdf2Sha256.gcm(SECRET_KEY);
    }

    @Override
    StringEncryptor legacyEncryptor() {
      return StringEncryptor.gcm(SECRET_KEY);
    }
  }

  @Nested
  @SuppressWarnings("deprecation")
  class CBC extends Base {
    @Override
    StringEncryptorCoordinator coordinator() {
      return StringEncryptorCoordinator.cbc(SECRET_KEY);
    }

    @Override
    StringEncryptorPbkdf2Sha256 v1Encryptor() {
      return StringEncryptorPbkdf2Sha256.cbc(SECRET_KEY);
    }

    @Override
    StringEncryptor legacyEncryptor() {
      return StringEncryptor.cbc(SECRET_KEY);
    }
  }
}
