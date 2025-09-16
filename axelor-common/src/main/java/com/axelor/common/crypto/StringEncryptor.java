/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.crypto;

import com.google.common.base.MoreObjects;
import com.google.common.io.BaseEncoding;
import java.nio.charset.StandardCharsets;

/** The {@link StringEncryptor} can be used to encrypt/decrypt {@link String} values. */
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
