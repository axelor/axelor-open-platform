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

@SuppressWarnings({"unchecked", "serial"})
public class ValueEnumType implements DynamicParameterizedType, UserType, Serializable {

  private int sqlType;

  private Class<? extends ValueEnum<?>> enumType;

  @Override
  public void setParameterValues(Properties parameters) {
    ParameterType params = (ParameterType) parameters.get(PARAMETER_TYPE);
    enumType = params.getReturnedClass();
    if (!enumType.isEnum()) {
      throw new RuntimeException("Not enum type " + enumType.getName());
    }
    final ValueEnum<?>[] enums = enumType.getEnumConstants();
    if (enums == null || enums.length == 0) {
      throw new RuntimeException("Invalid enum type " + enumType.getName());
    }
    if (enums[0].getValue() instanceof Integer) {
      sqlType = Types.INTEGER;
    } else {
      sqlType = Types.VARCHAR;
    }
  }

  @Override
  public int[] sqlTypes() {
    return new int[] {sqlType};
  }

  @Override
  public Class<? extends ValueEnum<?>> returnedClass() {
    return enumType;
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
      ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
      throws HibernateException, SQLException {
    final Object value = rs.getObject(names[0]);
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
