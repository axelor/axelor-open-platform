/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.crypto;

import com.google.common.base.MoreObjects;
import com.google.common.io.BaseEncoding;
import java.nio.charset.StandardCharsets;

/**
 * AES encryptor operating on {@link String} values.
 *
 * <p>Encrypts and decrypts strings by delegating to a {@link BytesEncryptor} and encoding the
 * resulting bytes as Base64.
 *
 * @deprecated Backed by {@link BytesEncryptor}, which uses weak key derivation (PBKDF2WithHmacSHA1,
 *     1024 iterations) and an instance-scoped salt. Use {@link StringEncryptorPbkdf2Sha512} (or
 *     {@link StringEncryptorPbkdf2Sha256}) for new encryptions, or {@link
 *     StringEncryptorCoordinator} to handle legacy {@code $AES$}, {@code $AESv1$}, and {@code
 *     $AESv2$} ciphertext transparently.
 */
@Deprecated
public class StringEncryptor implements Encryptor<String, String> {

  private final BaseEncoding encoder;

  private final BytesEncryptor encryptor;

  private final String prefix;

  public StringEncryptor(BytesEncryptor encryptor) {
    this.encryptor = encryptor;
    this.encoder = BaseEncoding.base64();
    this.prefix = this.encoder.omitPadding().encode(BytesEncryptor.PREFIX_BYTES);
  }

  public static StringEncryptor cbc(String password) {
    return new StringEncryptor(
        new BytesEncryptor(OperationMode.CBC, PaddingScheme.PKCS5, password));
  }

  public static StringEncryptor gcm(String password) {
    return new StringEncryptor(new BytesEncryptor(OperationMode.GCM, PaddingScheme.NONE, password));
  }

  @Override
  public boolean isEncrypted(String message) {
    if (message == null || message.length() < this.prefix.length()) {
      return false;
    }

    String prefix = message.substring(0, this.prefix.length());

    if (!this.encoder.canDecode(prefix)) {
      return false;
    }

    byte[] bytes = this.encoder.omitPadding().decode(prefix);

    prefix = new String(bytes, StandardCharsets.UTF_8);

    return BytesEncryptor.PREFIX.equals(prefix);
  }

  @Override
  public String encrypt(String message) {
    if (message == null || isEncrypted(message)) {
      return message;
    }
    byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
    return encoder.encode(encryptor.encrypt(bytes));
  }

  @Override
  public String decrypt(String encryptedMessage) {
    if (encryptedMessage == null || !isEncrypted(encryptedMessage)) {
      return encryptedMessage;
    }
    byte[] decrypted = encryptor.decrypt(encoder.decode(encryptedMessage));
    return new String(decrypted, StandardCharsets.UTF_8);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(StringEncryptor.class)
        .addValue(this.encryptor.getTransformation())
        .toString();
  }
}
