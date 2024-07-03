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

import com.axelor.db.internal.DBHelper;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.SqlTypes;
import org.hibernate.usertype.UserType;

public class JsonType implements UserType<String> {

  private static JsonTypeBase type =
      DBHelper.isHSQL() ? new JsonTypeLongVarChar() : new JsonTypeJson();

  @Override
  public int getSqlType() {
    return type.getSqlType();
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
    return rs.getString(position);
  }

  @Override
  public void nullSafeSet(
      PreparedStatement st, String value, int index, SharedSessionContractImplementor session)
      throws SQLException {
    type.nullSafeSet(st, value, index, session);
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

  private interface JsonTypeBase {
    int getSqlType();

    void nullSafeSet(
        PreparedStatement st, String value, int index, SharedSessionContractImplementor session)
        throws SQLException;
  }

  private static class JsonTypeLongVarChar implements JsonTypeBase {
    @Override
    public int getSqlType() {
      return Types.LONGVARCHAR;
    }

    @Override
    public void nullSafeSet(
        PreparedStatement st, String value, int index, SharedSessionContractImplementor session)
        throws SQLException {
      st.setString(index, value);
    }
  }

  private static class JsonTypeJson implements JsonTypeBase {
    @Override
    public int getSqlType() {
      return SqlTypes.JSON;
    }

    @Override
    public void nullSafeSet(
        PreparedStatement st, String value, int index, SharedSessionContractImplementor session)
        throws SQLException {
      if (value == null) {
        st.setNull(index, Types.OTHER);
      } else {
        st.setObject(index, value, Types.OTHER);
      }
    }
  }
}
