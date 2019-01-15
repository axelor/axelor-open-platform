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
package com.axelor.db.hibernate.type;

import com.axelor.db.converters.EncryptedStringConverter;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;

public class EncryptedStringTypeDescriptor extends StringTypeDescriptor {

  private static final long serialVersionUID = -6042582999686819489L;

  public static final EncryptedStringTypeDescriptor INSTANCE = new EncryptedStringTypeDescriptor();

  private static final EncryptedStringConverter CONVERTER = new EncryptedStringConverter();

  @Override
  public <X> X unwrap(String value, Class<X> type, WrapperOptions options) {
    String encrypted = CONVERTER.convertToDatabaseColumn(value);
    return super.unwrap(encrypted, type, options);
  }

  @Override
  public <X> String wrap(X value, WrapperOptions options) {
    String encrypted = super.wrap(value, options);
    return CONVERTER.convertToEntityAttribute(encrypted);
  }
}
