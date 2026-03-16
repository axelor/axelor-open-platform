/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2026 Axelor (<http://axelor.com>).
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
package com.axelor.db.converters;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.StringUtils;
import com.axelor.common.crypto.Encryptor;
import javax.persistence.AttributeConverter;

public abstract class AbstractEncryptedConverter<T, R> implements AttributeConverter<T, R> {

  private final String encryptionAlgorithm =
      AppSettings.get().get(AvailableAppSettings.ENCRYPTION_ALGORITHM);
  private final String encryptionPassword =
      AppSettings.get().get(AvailableAppSettings.ENCRYPTION_PASSWORD);

  private final String oldEncryptionAlgorithm =
      AppSettings.get().get(AvailableAppSettings.ENCRYPTION_OLD_ALGORITHM);
  private final String oldEncryptionPassword =
      AppSettings.get().get(AvailableAppSettings.ENCRYPTION_OLD_PASSWORD);

  private Encryptor<T, R> encryptor;
  private Encryptor<T, R> oldEncryptor;

  protected abstract Encryptor<T, R> getEncryptor(String algorithm, String password);

  protected final Encryptor<T, R> encryptor() {
    if (encryptor == null && StringUtils.notBlank(encryptionPassword)) {
      encryptor = getEncryptor(encryptionAlgorithm, encryptionPassword);
    }
    return encryptor;
  }

  protected final Encryptor<T, R> oldEncryptor() {
    if (oldEncryptor == null && StringUtils.notBlank(oldEncryptionPassword)) {
      oldEncryptor = getEncryptor(oldEncryptionAlgorithm, oldEncryptionPassword);
    }
    return oldEncryptor;
  }

  protected boolean isMigrating() {
    return "encrypt".equalsIgnoreCase(System.getProperty("axelor.task.database"));
  }

  @Override
  @SuppressWarnings("unchecked")
  public R convertToDatabaseColumn(T attribute) {
    Encryptor<T, R> e = encryptor();
    return e == null ? (R) attribute : e.encrypt(attribute);
  }

  @Override
  @SuppressWarnings("unchecked")
  public T convertToEntityAttribute(R dbData) {
    Encryptor<T, R> e = isMigrating() ? oldEncryptor() : encryptor();
    return e == null ? (T) dbData : e.decrypt(dbData);
  }
}
