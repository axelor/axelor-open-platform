/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Validates that encrypted values produced by each encryptor variant fit within a {@code
 * VARCHAR(255)} database column, and documents the maximum plaintext byte count for each variant.
 *
 * <p>The database value is a Base64 string of the binary payload. Base64 inflates size by 4/3, so
 * the binary payload must stay within 189 bytes to produce at most 252 Base64 characters (the
 * largest multiple of 4 that is ≤ 255).
 *
 * <p>Binary overhead per variant (fixed bytes added before the ciphertext):
 *
 * <pre>
 * Legacy SHA1 CBC:    prefix(5) + salt(8)                                    =  13 B  → max 175 bytes plaintext
 * Legacy SHA1 GCM:    prefix(5) + salt(8) + iv(16)                           =  29 B  → max 144 bytes plaintext (+ 16 B GCM tag)
 * SHA256/SHA512 GCM:  prefix(7) + iter(4) + sizes(2) + salt(16) + iv(12)     =  41 B  → max 132 bytes plaintext (+ 16 B GCM tag)
 * SHA256/SHA512 CBC:  prefix(7) + iter(4) + sizes(2) + salt(16) + iv(16)     =  45 B  → max 143 bytes plaintext
 * </pre>
 *
 * <p>Note: "max bytes plaintext" equals "max characters" only for single-byte encodings (ASCII,
 * Latin-1). Multi-byte UTF-8 characters reduce the effective character limit proportionally: 2-byte
 * characters (e.g. {@code é}) halve it, 3-byte (e.g. {@code 中}) divide by 3, etc.
 *
 * <p>SHA256 and SHA512 produce identical output sizes — they share the same binary layout and both
 * derive a 256-bit AES key. The only difference is the PBKDF2 algorithm and default iteration
 * count.
 */
@SuppressWarnings("deprecation")
public class TestEncryptedColumnSize {

  /** Maximum length of a standard database string column. */
  private static final int COLUMN_SIZE = 255;

  private static final String PASSWORD = "TestPassword";

  /**
   * Low iteration count used in size tests only. Output size is independent of iteration count, so
   * using 1 keeps tests fast without affecting correctness.
   */
  private static final int FAST_ITERATIONS = 1;

  // ---------------------------------------------------------------------------
  // Encryptor fixtures
  // ---------------------------------------------------------------------------

  /**
   * Arguments: (display name, encryptor, expected max plaintext bytes fitting in VARCHAR(255)).
   *
   * <p>The new SHA256 and SHA512 encryptors use {@link #FAST_ITERATIONS} so tests run quickly.
   * Production defaults (600 000 for SHA256, 210 000 for SHA512) do not change output sizes.
   */
  static Stream<Arguments> encryptors() {
    return Stream.of(
        Arguments.of("Legacy SHA1/CBC", StringEncryptor.cbc(PASSWORD), 175),
        Arguments.of("Legacy SHA1/GCM", StringEncryptor.gcm(PASSWORD), 144),
        Arguments.of(
            "SHA256/GCM (default)",
            new StringEncryptorPbkdf2Sha256(OperationMode.GCM, PASSWORD, FAST_ITERATIONS),
            132),
        Arguments.of(
            "SHA256/CBC",
            new StringEncryptorPbkdf2Sha256(OperationMode.CBC, PASSWORD, FAST_ITERATIONS),
            143),
        Arguments.of(
            "SHA512/GCM (default)",
            new StringEncryptorPbkdf2Sha512(OperationMode.GCM, PASSWORD, FAST_ITERATIONS),
            132),
        Arguments.of(
            "SHA512/CBC",
            new StringEncryptorPbkdf2Sha512(OperationMode.CBC, PASSWORD, FAST_ITERATIONS),
            143));
  }

  static Stream<Arguments> encryptorsGcm() {
    return Stream.of(
        Arguments.of("Legacy SHA1/GCM", StringEncryptor.gcm(PASSWORD), 144),
        Arguments.of(
            "SHA256/GCM",
            new StringEncryptorPbkdf2Sha256(OperationMode.GCM, PASSWORD, FAST_ITERATIONS),
            132),
        Arguments.of(
            "SHA512/GCM",
            new StringEncryptorPbkdf2Sha512(OperationMode.GCM, PASSWORD, FAST_ITERATIONS),
            132));
  }

  // ---------------------------------------------------------------------------
  // ASCII boundary tests
  // ---------------------------------------------------------------------------

  /**
   * The maximum ASCII plaintext encrypts to at most {@value #COLUMN_SIZE} characters and
   * round-trips correctly.
   */
  @ParameterizedTest(name = "[{0}] {2} ASCII chars encrypt to ≤ " + COLUMN_SIZE + " chars")
  @MethodSource("encryptors")
  void maxAsciiPlaintextFitsInColumn(
      String name, Encryptor<String, String> encryptor, int maxBytes) {
    final String plaintext = "a".repeat(maxBytes);
    final String encrypted = encryptor.encrypt(plaintext);

    assertTrue(
        encrypted.length() <= COLUMN_SIZE,
        "[%s] max plaintext of %d bytes encrypted to %d chars (expected ≤ %d)"
            .formatted(name, maxBytes, encrypted.length(), COLUMN_SIZE));
    assertEquals(plaintext, encryptor.decrypt(encrypted));
  }

  /**
   * One byte beyond the maximum plaintext produces an encrypted value that exceeds {@value
   * #COLUMN_SIZE} characters.
   */
  @ParameterizedTest(name = "[{0}] {2}+1 ASCII chars encrypt to > " + COLUMN_SIZE + " chars")
  @MethodSource("encryptors")
  void oneBeyondMaxExceedsColumn(String name, Encryptor<String, String> encryptor, int maxBytes) {
    final String plaintext = "a".repeat(maxBytes + 1);
    final String encrypted = encryptor.encrypt(plaintext);

    assertTrue(
        encrypted.length() > COLUMN_SIZE,
        "[%s] plaintext of %d bytes encrypted to %d chars (expected > %d)"
            .formatted(name, maxBytes + 1, encrypted.length(), COLUMN_SIZE));
  }

  /** Typical short values (10, 50, 100 bytes) all fit comfortably within the column limit. */
  @ParameterizedTest(name = "[{0}] short plaintexts all fit in VARCHAR(" + COLUMN_SIZE + ")")
  @MethodSource("encryptors")
  void typicalShortValuesFitInColumn(
      String name, Encryptor<String, String> encryptor, int maxBytes) {
    for (int size : new int[] {10, 50, 100}) {
      if (size > maxBytes) continue;
      final String plaintext = "a".repeat(size);
      final String encrypted = encryptor.encrypt(plaintext);

      assertTrue(
          encrypted.length() <= COLUMN_SIZE,
          "[%s] plaintext of %d bytes encrypted to %d chars (expected ≤ %d)"
              .formatted(name, size, encrypted.length(), COLUMN_SIZE));
      assertEquals(plaintext, encryptor.decrypt(encrypted));
    }
  }

  // ---------------------------------------------------------------------------
  // Multi-byte UTF-8 character tests
  // ---------------------------------------------------------------------------

  /**
   * 2-byte UTF-8 characters (e.g. {@code é}, U+00E9) halve the effective character limit because
   * each character occupies 2 bytes in UTF-8.
   *
   * <p>Max 2-byte characters = maxBytes / 2 (e.g. 66 for SHA256/GCM whose max is 132 bytes).
   */
  @ParameterizedTest(name = "[{0}] 2-byte chars (é): max chars fit in VARCHAR(" + COLUMN_SIZE + ")")
  @MethodSource("encryptors")
  void maxTwoByteCharsFitInColumn(String name, Encryptor<String, String> encryptor, int maxBytes) {
    final int maxChars = maxBytes / 2;
    final String plaintext = "é".repeat(maxChars);
    assertEquals(
        maxChars * 2,
        plaintext.getBytes(StandardCharsets.UTF_8).length,
        "Precondition: 'é' must be 2 UTF-8 bytes");

    final String encrypted = encryptor.encrypt(plaintext);

    assertTrue(
        encrypted.length() <= COLUMN_SIZE,
        "[%s] %d × 2-byte chars encrypted to %d chars (expected ≤ %d)"
            .formatted(name, maxChars, encrypted.length(), COLUMN_SIZE));
    assertEquals(plaintext, encryptor.decrypt(encrypted));
  }

  /**
   * One extra 2-byte character beyond the limit produces an encrypted value that exceeds {@value
   * #COLUMN_SIZE} characters.
   *
   * <p>Only tested for GCM encryptors because GCM grows strictly linearly: adding 2 bytes always
   * adds exactly the same number of Base64 characters. CBC's PKCS5 padding means the boundary
   * depends on block alignment and may require more than one extra character to overflow.
   */
  @ParameterizedTest(
      name = "[{0}] 2-byte chars (é): max+1 chars exceed VARCHAR(" + COLUMN_SIZE + ")")
  @MethodSource("encryptorsGcm")
  void oneBeyondMaxTwoByteCharsExceedsColumn(
      String name, Encryptor<String, String> encryptor, int maxBytes) {
    final int maxChars = maxBytes / 2;
    final String plaintext = "é".repeat(maxChars + 1);

    final String encrypted = encryptor.encrypt(plaintext);

    assertTrue(
        encrypted.length() > COLUMN_SIZE,
        "[%s] %d × 2-byte chars encrypted to %d chars (expected > %d)"
            .formatted(name, maxChars + 1, encrypted.length(), COLUMN_SIZE));
  }

  /**
   * 3-byte UTF-8 characters (e.g. {@code 中}, U+4E2D) reduce the effective character limit to
   * maxBytes / 3 (e.g. 44 for SHA256/GCM whose max is 132 bytes).
   */
  @ParameterizedTest(name = "[{0}] 3-byte chars (中): max chars fit in VARCHAR(" + COLUMN_SIZE + ")")
  @MethodSource("encryptors")
  void maxThreeByteCharsFitInColumn(
      String name, Encryptor<String, String> encryptor, int maxBytes) {
    final int maxChars = maxBytes / 3;
    final String plaintext = "中".repeat(maxChars);
    assertEquals(
        maxChars * 3,
        plaintext.getBytes(StandardCharsets.UTF_8).length,
        "Precondition: '中' must be 3 UTF-8 bytes");

    final String encrypted = encryptor.encrypt(plaintext);

    assertTrue(
        encrypted.length() <= COLUMN_SIZE,
        "[%s] %d × 3-byte chars encrypted to %d chars (expected ≤ %d)"
            .formatted(name, maxChars, encrypted.length(), COLUMN_SIZE));
    assertEquals(plaintext, encryptor.decrypt(encrypted));
  }
}
