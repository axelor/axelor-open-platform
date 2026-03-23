/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.converters;

import com.axelor.common.crypto.BytesEncryptorCoordinator;
import jakarta.persistence.Converter;

@Converter
public class EncryptedBytesConverter extends AbstractEncryptedConverter<byte[], byte[]> {

  @Override
  protected BytesEncryptorCoordinator getEncryptor(String algorithm, String password) {
    return "GCM".equalsIgnoreCase(algorithm)
        ? BytesEncryptorCoordinator.gcm(password)
        : BytesEncryptorCoordinator.cbc(password);
  }
}
