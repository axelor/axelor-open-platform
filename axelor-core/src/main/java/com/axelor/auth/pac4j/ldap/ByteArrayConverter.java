/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.auth.pac4j.ldap;

import com.axelor.common.ObjectUtils;
import java.util.Base64;
import org.pac4j.core.profile.converter.AbstractAttributeConverter;

public class ByteArrayConverter extends AbstractAttributeConverter<byte[]> {

  public static final ByteArrayConverter INSTANCE = new ByteArrayConverter();

  protected ByteArrayConverter() {
    super(byte[].class);
  }

  @Override
  protected byte[] internalConvert(Object attribute) {
    return ObjectUtils.isEmpty(attribute) ? null : Base64.getDecoder().decode(attribute.toString());
  }
}
