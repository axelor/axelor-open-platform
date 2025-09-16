/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.hibernate.type;

import com.axelor.db.converters.EncryptedStringConverter;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
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
    return Objects.equals(x, y);
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
