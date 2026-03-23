/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.crypto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Abstract base for AES string encryptors backed by a PBKDF2 key derivation function.
 *
 * @see StringEncryptorPbkdf2Sha256
 * @see StringEncryptorPbkdf2Sha512
 */
abstract class AbstractStringEncryptorPbkdf2 implements Encryptor<String, String> {

  private final AbstractBytesEncryptorPbkdf2 encryptor;
  private final String rawPrefix;
  private final String b64Prefix;

  /**
   * @param encryptor the underlying bytes encryptor
   * @param rawPrefix the raw prefix string used to identify ciphertext
   */
  protected AbstractStringEncryptorPbkdf2(
      AbstractBytesEncryptorPbkdf2 encryptor, String rawPrefix) {
    this.encryptor = encryptor;
    this.rawPrefix = rawPrefix;
    this.b64Prefix =
        Base64.getEncoder()
            .withoutPadding()
            .encodeToString(rawPrefix.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public boolean isEncrypted(String message) {
    if (message == null || message.length() < this.b64Prefix.length()) {
      return false;
    }
    try {
      final String fragment = message.substring(0, this.b64Prefix.length());
      final int padCount = (4 - fragment.length() % 4) % 4;
      final byte[] bytes = Base64.getDecoder().decode(fragment + "=".repeat(padCount));
      return rawPrefix.equals(new String(bytes, StandardCharsets.UTF_8));
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  @Override
  public String encrypt(String message) {
    if (message == null || isEncrypted(message)) {
      return message;
    }
    return Base64.getEncoder()
        .encodeToString(encryptor.encrypt(message.getBytes(StandardCharsets.UTF_8)));
  }

  @Override
  public String decrypt(String encryptedMessage) {
    if (encryptedMessage == null || !isEncrypted(encryptedMessage)) {
      return encryptedMessage;
    }
    return new String(
        encryptor.decrypt(Base64.getDecoder().decode(encryptedMessage)), StandardCharsets.UTF_8);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" + encryptor.getTransformation() + "}";
  }
}
