/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.crypto;

/**
 * The {@link Encryptor} interface.
 *
 * @param <T> the input type
 * @param <R> the return type
 */
public interface Encryptor<T, R> {

  /**
   * Check whether the given message is already encrypted or not.
   *
   * @param message the message to check
   * @return true if encrypted
   */
  boolean isEncrypted(T message);

  /**
   * Encrypt the given message.
   *
   * @param message the message to encrypt
   * @return encrypted message
   */
  R encrypt(T message);

  /**
   * Decrypt the given encrypted message.
   *
   * @param encryptedMessage the encrypted message to decrypt
   * @return decrypted message
   */
  T decrypt(R encryptedMessage);
}
