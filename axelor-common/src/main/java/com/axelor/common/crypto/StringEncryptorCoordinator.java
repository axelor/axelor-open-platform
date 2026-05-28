/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.crypto;

/**
 * Coordinator that routes encrypt/decrypt operations between the legacy {@link StringEncryptor}
 * ({@code $AES$}), {@link StringEncryptorPbkdf2Sha256} ({@code $AESv1$}), and {@link
 * StringEncryptorPbkdf2Sha512} ({@code $AESv2$}).
 *
 * <p>All new encryptions always use the {@code $AESv1$} format. Decryption automatically dispatches
 * based on the version prefix embedded in the base64 payload.
 */
@SuppressWarnings("deprecation")
public class StringEncryptorCoordinator implements Encryptor<String, String> {

  private final StringEncryptorPbkdf2Sha512 v2Encryptor;
  private final StringEncryptorPbkdf2Sha256 v1Encryptor;
  private final StringEncryptor legacyEncryptor;

  /**
   * Creates a coordinator with explicit modes for both the current (v1/v2) and legacy encryptors.
   *
   * @param mode the operation mode used by the encryptors
   * @param password the encryption password
   */
  public StringEncryptorCoordinator(OperationMode mode, String password) {
    this.v1Encryptor =
        new StringEncryptorPbkdf2Sha256(new BytesEncryptorPbkdf2Sha256(mode, password));
    this.v2Encryptor =
        new StringEncryptorPbkdf2Sha512(new BytesEncryptorPbkdf2Sha512(mode, password));
    this.legacyEncryptor =
        new StringEncryptor(
            new BytesEncryptor(mode, AbstractBytesEncryptorPbkdf2.paddingFor(mode), password));
  }

  /** Creates a coordinator using AES/GCM for current encryptions. */
  public static StringEncryptorCoordinator gcm(String password) {
    return new StringEncryptorCoordinator(OperationMode.GCM, password);
  }

  /** Creates a coordinator using AES/CBC for current encryptions. */
  public static StringEncryptorCoordinator cbc(String password) {
    return new StringEncryptorCoordinator(OperationMode.CBC, password);
  }

  @Override
  public boolean isEncrypted(String message) {
    return v1Encryptor.isEncrypted(message)
        || v2Encryptor.isEncrypted(message)
        || legacyEncryptor.isEncrypted(message);
  }

  /** Always encrypts using the current {@code $AESv1$} format. */
  @Override
  public String encrypt(String message) {
    if (message == null || isEncrypted(message)) {
      return message;
    }
    return v1Encryptor.encrypt(message);
  }

  /** Decrypts by dispatching to the appropriate bundle based on the version prefix. */
  @Override
  public String decrypt(String encryptedMessage) {
    if (v1Encryptor.isEncrypted(encryptedMessage)) {
      return v1Encryptor.decrypt(encryptedMessage);
    }
    if (v2Encryptor.isEncrypted(encryptedMessage)) {
      return v2Encryptor.decrypt(encryptedMessage);
    }
    if (legacyEncryptor.isEncrypted(encryptedMessage)) {
      return legacyEncryptor.decrypt(encryptedMessage);
    }
    return encryptedMessage;
  }
}
