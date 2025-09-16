/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.hibernate.type;

import com.axelor.db.ValueEnum;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.UserType;

public class ValueEnumType implements UserType<Object>, DynamicParameterizedType {

  private int sqlType;
  private Class<?> clazz;

  @Override
  public int getSqlType() {
    return sqlType;
  }

  @Override
  public Class<Object> returnedClass() {
    return (Class<Object>) clazz;
  }

  @Override
  public void setParameterValues(Properties parameters) {
    ParameterType params = (ParameterType) parameters.get(PARAMETER_TYPE);
    clazz = params.getReturnedClass();
    if (!clazz.isEnum()) {
      throw new RuntimeException("Not enum type " + clazz.getName());
    }
    final ValueEnum<?>[] enums = (ValueEnum<?>[]) clazz.getEnumConstants();
    if (enums == null || enums.length == 0) {
      throw new RuntimeException("Invalid enum type " + clazz.getName());
    }
    if (enums[0].getValue() instanceof Integer) {
      sqlType = Types.INTEGER;
    } else {
      sqlType = Types.VARCHAR;
    }
  }

  @Override
  public boolean equals(Object x, Object y) throws HibernateException {
    return x == y;
  }

  @Override
  public int hashCode(Object x) throws HibernateException {
    return x == null ? 0 : x.hashCode();
  }

  @Override
  public Object nullSafeGet(
      ResultSet rs, int i, SharedSessionContractImplementor session, Object owner)
      throws SQLException {
    final Object value = rs.getObject(i);
    return rs.wasNull() ? null : ValueEnum.of(returnedClass().asSubclass(Enum.class), value);
  }

  @Override
  public void nullSafeSet(
      PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
      throws HibernateException, SQLException {
    if (value == null) {
      st.setNull(index, sqlType);
    } else {
      st.setObject(index, ((ValueEnum<?>) value).getValue());
    }
  }

  @Override
  public Object deepCopy(Object value) throws HibernateException {
    return value;
  }

  @Override
  public boolean isMutable() {
    return false;
  }

  @Override
  public Serializable disassemble(Object value) throws HibernateException {
    return (Serializable) value;
  }

  @Override
  public Object assemble(Serializable cached, Object owner) throws HibernateException {
    return cached;
  }

  @Override
  public Object replace(Object original, Object target, Object owner) throws HibernateException {
    return original;
  }
}
