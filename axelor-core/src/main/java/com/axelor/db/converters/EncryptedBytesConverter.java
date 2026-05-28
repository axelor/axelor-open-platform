/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.converters;

import com.axelor.common.crypto.BytesEncryptorCoordinator;
import com.axelor.common.crypto.OperationMode;
import jakarta.persistence.Converter;

@Converter
public class EncryptedBytesConverter extends AbstractEncryptedConverter<byte[], byte[]> {

  @Override
  protected BytesEncryptorCoordinator getEncryptor(String algorithm, String password) {
    OperationMode mode =
        OperationMode.CBC.name().equalsIgnoreCase(algorithm)
            ? OperationMode.CBC
            : OperationMode.GCM;
    return new BytesEncryptorCoordinator(mode, password);
  }
}
