/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.converters;

import com.axelor.common.crypto.BytesEncryptor;
import jakarta.persistence.Converter;

@Converter
public class EncryptedBytesConverter extends AbstractEncryptedConverter<byte[], byte[]> {

  @Override
  protected BytesEncryptor getEncryptor(String algorithm, String password) {
    return "GCM".equalsIgnoreCase(algorithm)
        ? BytesEncryptor.gcm(password)
        : BytesEncryptor.cbc(password);
  }
}
