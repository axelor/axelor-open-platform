/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.converters;

import com.axelor.common.crypto.StringEncryptorCoordinator;
import jakarta.persistence.Converter;

@Converter
public class EncryptedStringConverter extends AbstractEncryptedConverter<String, String> {

  @Override
  protected StringEncryptorCoordinator getEncryptor(String algorithm, String password) {
    return "GCM".equalsIgnoreCase(algorithm)
        ? StringEncryptorCoordinator.gcm(password)
        : StringEncryptorCoordinator.cbc(password);
  }
}
