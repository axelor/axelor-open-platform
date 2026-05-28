/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.crypto;

/**
 * AES encryptor operating on raw byte arrays using PBKDF2WithHmacSHA256 key derivation, supporting
 * CBC and GCM operation modes.
 *
 * <p>A fresh 16-byte salt and IV are generated for every {@link #encrypt(byte[])} call, ensuring
 * unique ciphertext even for identical plaintext with the same password.
 *
 * <p>Both CBC and GCM modes share the same binary payload layout. The IV is always randomly
 * generated and stored in the payload regardless of the mode. The IV size differs per mode: 12
 * bytes for GCM (NIST SP 800-38D recommendation) and 16 bytes for CBC (AES block size):
 *
 * <pre>
 * | $AESv1$ (7 bytes) | iterations (4 bytes) | salt_size (1 byte) | iv_size (1 byte) | salt (n bytes) | iv (n bytes) | encrypted_data |
 * </pre>
 *
 * <p>Use the factory methods to select the AES operation mode:
 *
 * <ul>
 *   <li>{@link #gcm(String)} — AES/GCM/NoPadding (recommended: provides authenticated encryption)
 *   <li>{@link #cbc(String)} — AES/CBC/PKCS5Padding
 * </ul>
 *
 * <p>The default constructor uses GCM.
 */
public class BytesEncryptorPbkdf2Sha256 extends AbstractBytesEncryptorPbkdf2 {

  public static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
  public static final int DEFAULT_ITERATIONS = 600_000;

  public static final String PREFIX = "$AESv1$";
  static final byte[] PREFIX_BYTES = PREFIX.getBytes();

  /** Creates a GCM encryptor with the given password. */
  public BytesEncryptorPbkdf2Sha256(String password) {
    this(OperationMode.GCM, password);
  }

  /** Creates an encryptor for the given mode and password */
  public BytesEncryptorPbkdf2Sha256(OperationMode mode, String password) {
    this(mode, password, DEFAULT_ITERATIONS);
  }

  /**
   * Creates an encryptor for the given mode, password, and iteration count.
   *
   * @param mode the AES operation mode (GCM or CBC)
   * @param password the password used for PBKDF2 key derivation
   * @param iterations the PBKDF2 iteration count
   */
  public BytesEncryptorPbkdf2Sha256(OperationMode mode, String password, int iterations) {
    super(mode, password, KEY_ALGORITHM, iterations, PREFIX_BYTES);
  }

  /**
   * Creates an encryptor with a custom salt size.
   *
   * @param mode the AES operation mode (GCM or CBC)
   * @param password the password used for PBKDF2 key derivation
   * @param iterations the PBKDF2 iteration count
   * @param saltSize the salt length in bytes
   */
  public BytesEncryptorPbkdf2Sha256(
      OperationMode mode, String password, int iterations, int saltSize) {
    super(mode, password, KEY_ALGORITHM, iterations, PREFIX_BYTES, saltSize);
  }

  /** Creates a GCM encryptor with the given password. */
  public static BytesEncryptorPbkdf2Sha256 gcm(String password) {
    return new BytesEncryptorPbkdf2Sha256(OperationMode.GCM, password);
  }

  /** Creates a CBC encryptor with the given password . */
  public static BytesEncryptorPbkdf2Sha256 cbc(String password) {
    return new BytesEncryptorPbkdf2Sha256(OperationMode.CBC, password);
  }
}
