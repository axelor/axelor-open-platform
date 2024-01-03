/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
