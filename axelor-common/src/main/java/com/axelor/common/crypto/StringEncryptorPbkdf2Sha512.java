/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.crypto;

/**
 * AES encryptor operating on {@link String} values using PBKDF2WithHmacSHA512 key derivation.
 *
 * <p>Encrypts and decrypts strings by delegating to a {@link BytesEncryptorPbkdf2Sha512} and
 * encoding the resulting bytes as Base64. The encrypted output starts with the Base64 encoding of
 * {@code $AESv2$}, which is used by {@link #isEncrypted(String)} to identify ciphertext.
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
public class StringEncryptorPbkdf2Sha512 extends AbstractStringEncryptorPbkdf2 {

  /** Creates a GCM encryptor with the given password. */
  public StringEncryptorPbkdf2Sha512(String password) {
    this(new BytesEncryptorPbkdf2Sha512(password));
  }

  /**
   * Creates an encryptor for the given mode and password.
   *
   * @param mode the AES operation mode (GCM or CBC)
   * @param password the password used for PBKDF2 key derivation
   */
  public StringEncryptorPbkdf2Sha512(OperationMode mode, String password) {
    this(new BytesEncryptorPbkdf2Sha512(mode, password));
  }

  /**
   * Creates an encryptor for the given mode, password, and iteration count.
   *
   * @param mode the AES operation mode (GCM or CBC)
   * @param password the password used for PBKDF2 key derivation
   * @param iterations the PBKDF2 iteration count
   */
  public StringEncryptorPbkdf2Sha512(OperationMode mode, String password, int iterations) {
    this(new BytesEncryptorPbkdf2Sha512(mode, password, iterations));
  }

  /**
   * Creates an encryptor with a custom salt size.
   *
   * @param mode the AES operation mode (GCM or CBC)
   * @param password the password used for PBKDF2 key derivation
   * @param iterations the PBKDF2 iteration count
   * @param saltSize the salt length in bytes
   */
  public StringEncryptorPbkdf2Sha512(
      OperationMode mode, String password, int iterations, int saltSize) {
    this(new BytesEncryptorPbkdf2Sha512(mode, password, iterations, saltSize));
  }

  /**
   * Creates a string encryptor backed by the given bytes encryptor.
   *
   * @param encryptor the underlying {@link BytesEncryptorPbkdf2Sha512}
   */
  public StringEncryptorPbkdf2Sha512(BytesEncryptorPbkdf2Sha512 encryptor) {
    super(encryptor, BytesEncryptorPbkdf2Sha512.PREFIX);
  }

  /** Creates a GCM encryptor with the given password. */
  public static StringEncryptorPbkdf2Sha512 gcm(String password) {
    return new StringEncryptorPbkdf2Sha512(BytesEncryptorPbkdf2Sha512.gcm(password));
  }

  /** Creates a CBC encryptor with the given password. */
  public static StringEncryptorPbkdf2Sha512 cbc(String password) {
    return new StringEncryptorPbkdf2Sha512(BytesEncryptorPbkdf2Sha512.cbc(password));
  }
}
