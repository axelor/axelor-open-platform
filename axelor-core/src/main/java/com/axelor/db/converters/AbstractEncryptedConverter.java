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

  private final String explicitOldEncryptionAlgorithm =
      AppSettings.get().get(AvailableAppSettings.ENCRYPTION_OLD_ALGORITHM);
  private final String explicitOldEncryptionPassword =
      AppSettings.get().get(AvailableAppSettings.ENCRYPTION_OLD_PASSWORD);

  // Only apply defaults when at least one old-* setting is explicitly configured.
  // If neither is set, the old encryptor stays null so plain-text values pass through as-is
  // (initial encryption use case).
  private final boolean hasOldEncryptionSettings =
      StringUtils.notBlank(explicitOldEncryptionAlgorithm)
          || StringUtils.notBlank(explicitOldEncryptionPassword);

  private final String oldEncryptionAlgorithm =
      hasOldEncryptionSettings
          ? (StringUtils.notBlank(explicitOldEncryptionAlgorithm)
              ? explicitOldEncryptionAlgorithm
              : encryptionAlgorithm)
          : null;
  private final String oldEncryptionPassword =
      hasOldEncryptionSettings
          ? (StringUtils.notBlank(explicitOldEncryptionPassword)
              ? explicitOldEncryptionPassword
              : encryptionPassword)
          : null;

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
