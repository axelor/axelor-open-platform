/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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

import javax.persistence.AttributeConverter;

import com.axelor.app.AppSettings;
import com.axelor.common.StringUtils;
import com.axelor.common.crypto.Encryptor;

public abstract class AbstractEncryptedConverter<T, R> implements AttributeConverter<T, R> {

	private static final String ENCRYPTION_ALGORITHM = AppSettings.get().get("encryption.algorithm");
	private static final String ENCRYPTION_PASSWORD = AppSettings.get().get("encryption.password");

	private Encryptor<T, R> encryptor;

	protected abstract Encryptor<T, R> getEncryptor(String algorithm, String password);

	protected final Encryptor<T, R> encryptor() {
		if (encryptor == null && StringUtils.notBlank(ENCRYPTION_PASSWORD)) {
			encryptor = getEncryptor(ENCRYPTION_ALGORITHM, ENCRYPTION_PASSWORD);
		}
		return encryptor;
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
		Encryptor<T, R> e = encryptor();
		return e == null ? (T) dbData : e.decrypt(dbData);
	}
}
