/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
package com.axelor.db.hibernate.type;

import com.axelor.db.converters.EncryptedStringConverter;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

public class EncryptedTextType implements UserType<String> {

  private static final EncryptedStringConverter CONVERTER = new EncryptedStringConverter();

  public EncryptedTextType() {}

  @Override
  public int getSqlType() {
    return Types.LONGVARCHAR;
  }

  @Override
  public Class<String> returnedClass() {
    return String.class;
  }

  @Override
  public boolean equals(String x, String y) {
    return (x == y) || (x != null && x.equals(y));
  }

  @Override
  public int hashCode(String x) {
    return x.hashCode();
  }

  @Override
  public String nullSafeGet(
      ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
      throws SQLException {
    String value = rs.getString(position);
    return (value == null || value.isEmpty()) ? null : CONVERTER.convertToEntityAttribute(value);
  }

  @Override
  public void nullSafeSet(
      PreparedStatement st, String value, int index, SharedSessionContractImplementor session)
      throws SQLException {
    if (value == null) {
      st.setNull(index, Types.VARCHAR);
    } else {
      st.setString(index, CONVERTER.convertToDatabaseColumn(value));
    }
  }

  @Override
  public String deepCopy(String value) {
    return value;
  }

  @Override
  public boolean isMutable() {
    return false;
  }

  @Override
  public Serializable disassemble(String value) {
    return value;
  }

  @Override
  public String assemble(Serializable cached, Object owner) {
    return (String) cached;
  }
}
