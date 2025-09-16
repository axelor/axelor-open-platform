/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.converters;

import com.axelor.common.crypto.StringEncryptor;
import jakarta.persistence.Converter;

@Converter
public class EncryptedStringConverter extends AbstractEncryptedConverter<String, String> {

  @Override
  protected StringEncryptor getEncryptor(String algorithm, String password) {
    return "GCM".equalsIgnoreCase(algorithm)
        ? StringEncryptor.gcm(password)
        : StringEncryptor.cbc(password);
  }
}
