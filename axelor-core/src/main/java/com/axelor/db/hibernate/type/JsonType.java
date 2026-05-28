/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
