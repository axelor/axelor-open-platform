/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.crypto;

/**
 * Coordinator that routes encrypt/decrypt operations between the legacy {@link BytesEncryptor}
 * ({@code $AES$}), {@link BytesEncryptorPbkdf2Sha256} ({@code $AESv1$}), and {@link
 * BytesEncryptorPbkdf2Sha512} ({@code $AESv2$}).
 *
 * <p>All new encryptions always use the {@code $AESv1$} format. Decryption automatically dispatches
 * based on the version prefix embedded in the payload.
 */
@SuppressWarnings("deprecation")
public class BytesEncryptorCoordinator implements Encryptor<byte[], byte[]> {

  private final BytesEncryptorPbkdf2Sha512 v2Encryptor;
  private final BytesEncryptorPbkdf2Sha256 v1Encryptor;
  private final BytesEncryptor legacyEncryptor;

  /**
   * Creates a coordinator with explicit modes for both the current (v1/v2) and legacy encryptors.
   *
   * @param mode the operation mode used by the encryptors
   * @param password the encryption password
   */
  public BytesEncryptorCoordinator(OperationMode mode, String password) {
    this.v1Encryptor = new BytesEncryptorPbkdf2Sha256(mode, password);
    this.v2Encryptor = new BytesEncryptorPbkdf2Sha512(mode, password);
    this.legacyEncryptor =
        new BytesEncryptor(mode, AbstractBytesEncryptorPbkdf2.paddingFor(mode), password);
  }

  /** Creates a coordinator using AES/GCM for current encryptions. */
  public static BytesEncryptorCoordinator gcm(String password) {
    return new BytesEncryptorCoordinator(OperationMode.GCM, password);
  }

  /** Creates a coordinator using AES/CBC for current encryptions. */
  public static BytesEncryptorCoordinator cbc(String password) {
    return new BytesEncryptorCoordinator(OperationMode.CBC, password);
  }

  @Override
  public boolean isEncrypted(byte[] bytes) {
    return v1Encryptor.isEncrypted(bytes)
        || v2Encryptor.isEncrypted(bytes)
        || legacyEncryptor.isEncrypted(bytes);
  }

  /** Always encrypts using the current {@code $AESv1$} format. */
  @Override
  public byte[] encrypt(byte[] bytes) {
    if (bytes == null || isEncrypted(bytes)) {
      return bytes;
    }
    return v1Encryptor.encrypt(bytes);
  }

  /** Decrypts by dispatching to the appropriate bundle based on the version prefix. */
  @Override
  public byte[] decrypt(byte[] bytes) {
    if (v1Encryptor.isEncrypted(bytes)) {
      return v1Encryptor.decrypt(bytes);
    }
    if (v2Encryptor.isEncrypted(bytes)) {
      return v2Encryptor.decrypt(bytes);
    }
    if (legacyEncryptor.isEncrypted(bytes)) {
      return legacyEncryptor.decrypt(bytes);
    }
    return bytes;
  }
}
