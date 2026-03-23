/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the binary payload format is self-describing: iterations, salt_size, and iv_size
 * are stored in the header, so changing these defaults in a future version does not break
 * decryption of existing ciphertext.
 */
public class TestBytesEncryptorPbkdf2SelfDescribing {

  private static final String PASSWORD = "MySuperSecretKey";
  private static final byte[] PLAINTEXT =
      "Hello, self-describing payload!".getBytes(StandardCharsets.UTF_8);

  /**
   * Test-only encryptor backed by PBKDF2WithHmacSHA256 with fully configurable iterations, salt
   * size, and IV size.
   */
  private static class ConfigurableEncryptor extends AbstractBytesEncryptorPbkdf2 {
    ConfigurableEncryptor(OperationMode mode, int iterations) {
      super(
          mode,
          PASSWORD,
          BytesEncryptorPbkdf2Sha256.KEY_ALGORITHM,
          iterations,
          BytesEncryptorPbkdf2Sha256.PREFIX_BYTES);
    }

    ConfigurableEncryptor(OperationMode mode, int iterations, int saltSize) {
      super(
          mode,
          PASSWORD,
          BytesEncryptorPbkdf2Sha256.KEY_ALGORITHM,
          iterations,
          BytesEncryptorPbkdf2Sha256.PREFIX_BYTES,
          saltSize);
    }

    ConfigurableEncryptor(OperationMode mode, int iterations, int saltSize, int ivSize) {
      super(
          mode,
          PASSWORD,
          BytesEncryptorPbkdf2Sha256.KEY_ALGORITHM,
          iterations,
          BytesEncryptorPbkdf2Sha256.PREFIX_BYTES,
          saltSize,
          ivSize);
    }
  }

  // -------------------------------------------------------------------------
  // Iterations
  // -------------------------------------------------------------------------

  @Test
  void testIterationsStoredInHeader_gcm() {
    assertIterationsStoredInHeader(OperationMode.GCM);
  }

  @Test
  void testIterationsStoredInHeader_cbc() {
    assertIterationsStoredInHeader(OperationMode.CBC);
  }

  private void assertIterationsStoredInHeader(OperationMode mode) {
    final int oldIterations = 1_000;
    final int newIterations = 2_000;

    final ConfigurableEncryptor oldEncryptor = new ConfigurableEncryptor(mode, oldIterations);
    final byte[] ciphertext = oldEncryptor.encrypt(PLAINTEXT);

    // Verify the iteration count is embedded in the payload header
    final ByteBuffer buf = ByteBuffer.wrap(ciphertext);
    buf.position(BytesEncryptorPbkdf2Sha256.PREFIX_BYTES.length);
    assertEquals(oldIterations, buf.getInt(), "iterations must be stored in the payload header");

    // An encryptor with a different default iteration count must still decrypt old ciphertext,
    // because it reads the iteration count from the payload rather than its own default
    final ConfigurableEncryptor newEncryptor = new ConfigurableEncryptor(mode, newIterations);
    assertArrayEquals(
        PLAINTEXT,
        newEncryptor.decrypt(ciphertext),
        "decryption must succeed after an iteration count change");
  }

  // -------------------------------------------------------------------------
  // Salt size
  // -------------------------------------------------------------------------

  @Test
  void testSaltSizeStoredInHeader_gcm() {
    assertSaltSizeStoredInHeader(OperationMode.GCM);
  }

  @Test
  void testSaltSizeStoredInHeader_cbc() {
    assertSaltSizeStoredInHeader(OperationMode.CBC);
  }

  private void assertSaltSizeStoredInHeader(OperationMode mode) {
    final int oldSaltSize = 8;
    final int newSaltSize = 127;

    final ConfigurableEncryptor oldEncryptor = new ConfigurableEncryptor(mode, 1_000, oldSaltSize);
    final byte[] ciphertext = oldEncryptor.encrypt(PLAINTEXT);

    // Verify salt_size and iv_size are embedded in the payload header
    final ByteBuffer buf = ByteBuffer.wrap(ciphertext);
    buf.position(BytesEncryptorPbkdf2Sha256.PREFIX_BYTES.length);
    buf.getInt(); // skip iterations
    assertEquals(
        oldSaltSize,
        Byte.toUnsignedInt(buf.get()),
        "salt_size must be stored in the payload header");

    // An encryptor with a different default salt size must still decrypt old ciphertext,
    // because it reads the salt_size from the payload rather than its own default
    final ConfigurableEncryptor newEncryptor = new ConfigurableEncryptor(mode, 1_000, newSaltSize);
    assertArrayEquals(
        PLAINTEXT,
        newEncryptor.decrypt(ciphertext),
        "decryption must succeed after a salt size change");
  }

  // -------------------------------------------------------------------------
  // IV size
  // -------------------------------------------------------------------------

  @Test
  void testIvSizeStoredInHeader_gcm() {
    // GCM IV can be any size; use a hypothetical old default of 16 vs the current 12
    assertIvSizeStoredInHeader(
        OperationMode.GCM, 16, AbstractBytesEncryptorPbkdf2.DEFAULT_GCM_IV_SIZE);
  }

  @Test
  void testIvSizeStoredInHeader_cbc() {
    // CBC IV must always be the AES block size (16); verify a future value of 16 still decrypts old
    // ciphertext
    assertIvSizeStoredInHeader(
        OperationMode.CBC,
        AbstractBytesEncryptorPbkdf2.DEFAULT_CBC_IV_SIZE,
        AbstractBytesEncryptorPbkdf2.DEFAULT_CBC_IV_SIZE);
  }

  private void assertIvSizeStoredInHeader(OperationMode mode, int oldIvSize, int newIvSize) {
    final ConfigurableEncryptor oldEncryptor =
        new ConfigurableEncryptor(mode, 1_000, 16, oldIvSize);
    final byte[] ciphertext = oldEncryptor.encrypt(PLAINTEXT);

    // Verify iv_size is embedded in the payload header
    final ByteBuffer buf = ByteBuffer.wrap(ciphertext);
    buf.position(BytesEncryptorPbkdf2Sha256.PREFIX_BYTES.length);
    buf.getInt(); // skip iterations
    buf.get(); // skip salt_size
    assertEquals(
        oldIvSize, Byte.toUnsignedInt(buf.get()), "iv_size must be stored in the payload header");

    // An encryptor with a different default IV size must still decrypt old ciphertext,
    // because it reads the iv_size from the payload rather than its own default
    final ConfigurableEncryptor newEncryptor =
        new ConfigurableEncryptor(mode, 1_000, 16, newIvSize);
    assertArrayEquals(
        PLAINTEXT,
        newEncryptor.decrypt(ciphertext),
        "decryption must succeed after an IV size change");
  }
}
