/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.db.converters;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.StringUtils;
import com.axelor.common.crypto.Encryptor;
import javax.persistence.AttributeConverter;

public abstract class AbstractEncryptedConverter<T, R> implements AttributeConverter<T, R> {

  private static final String ENCRYPTION_ALGORITHM =
      AppSettings.get().get(AvailableAppSettings.ENCRYPTION_ALGORITHM);
  private static final String ENCRYPTION_PASSWORD =
      AppSettings.get().get(AvailableAppSettings.ENCRYPTION_PASSWORD);

  private static final String OLD_ENCRYPTION_ALGORITHM =
      AppSettings.get().get(AvailableAppSettings.ENCRYPTION_ALGORITHM_OLD);
  private static final String OLD_ENCRYPTION_PASSWORD =
      AppSettings.get().get(AvailableAppSettings.ENCRYPTION_PASSWORD_OLD);

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
    return "true".equalsIgnoreCase(System.getProperty("database.encrypt.migrate"));
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
