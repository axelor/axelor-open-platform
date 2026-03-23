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

  private final String encryptionAlgorithm =
      AppSettings.get().get(AvailableAppSettings.ENCRYPTION_ALGORITHM);
  private final String encryptionPassword =
      AppSettings.get().get(AvailableAppSettings.ENCRYPTION_PASSWORD);

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
    if (oldEncryptor == null && isMigrating()) {
      String oldAlgorithm =
          StringUtils.notBlank(AppSettings.get().get(AvailableAppSettings.ENCRYPTION_OLD_ALGORITHM))
              ? AppSettings.get().get(AvailableAppSettings.ENCRYPTION_OLD_ALGORITHM)
              : encryptionAlgorithm;
      String oldPassword =
          StringUtils.notBlank(AppSettings.get().get(AvailableAppSettings.ENCRYPTION_OLD_PASSWORD))
              ? AppSettings.get().get(AvailableAppSettings.ENCRYPTION_OLD_PASSWORD)
              : encryptionPassword;
      if (StringUtils.notBlank(oldPassword)) {
        oldEncryptor = getEncryptor(oldAlgorithm, oldPassword);
      }
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
