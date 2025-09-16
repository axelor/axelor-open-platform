/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.converters;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.StringUtils;
import com.axelor.common.crypto.Encryptor;
import jakarta.persistence.AttributeConverter;

public abstract class AbstractEncryptedConverter<T, R> implements AttributeConverter<T, R> {

  private static final String ENCRYPTION_ALGORITHM =
      AppSettings.get().get(AvailableAppSettings.ENCRYPTION_ALGORITHM);
  private static final String ENCRYPTION_PASSWORD =
      AppSettings.get().get(AvailableAppSettings.ENCRYPTION_PASSWORD);

  private static final String OLD_ENCRYPTION_ALGORITHM =
      AppSettings.get().get(AvailableAppSettings.ENCRYPTION_OLD_ALGORITHM);
  private static final String OLD_ENCRYPTION_PASSWORD =
      AppSettings.get().get(AvailableAppSettings.ENCRYPTION_OLD_PASSWORD);

  private Encryptor<T, R> encryptor;
  private Encryptor<T, R> oldEncryptor;

  protected abstract Encryptor<T, R> getEncryptor(String algorithm, String password);

  protected final Encryptor<T, R> encryptor() {
    if (encryptor == null && StringUtils.notBlank(ENCRYPTION_PASSWORD)) {
      encryptor = getEncryptor(ENCRYPTION_ALGORITHM, ENCRYPTION_PASSWORD);
    }
    return encryptor;
  }

  protected final Encryptor<T, R> oldEncryptor() {
    if (oldEncryptor == null && StringUtils.notBlank(OLD_ENCRYPTION_PASSWORD)) {
      oldEncryptor = getEncryptor(OLD_ENCRYPTION_ALGORITHM, OLD_ENCRYPTION_PASSWORD);
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
