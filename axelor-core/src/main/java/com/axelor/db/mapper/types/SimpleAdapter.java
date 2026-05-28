/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.mapper.types;

import com.axelor.db.mapper.TypeAdapter;
import jakarta.persistence.Column;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

public class SimpleAdapter implements TypeAdapter<Object> {

  @Override
  public Object adapt(Object value, Class<?> type, Type genericType, Annotation[] annotations) {

    if (value == null || (value instanceof String string && "".equals(string.trim()))) {
      return adaptNull(value, type, genericType, annotations);
    }

    // handle Boolean first to avoid constructor as in that case equal comparison
    // using (==) with Boolean.TRUE, Boolean.FALSE will fail.
    if (type == Boolean.TYPE || type == Boolean.class) {
      return Boolean.valueOf(value.toString());
    }

    // try a constructor with exact value type
    try {
      return type.getConstructor(new Class<?>[] {value.getClass()})
          .newInstance(new Object[] {value});
    } catch (Exception e) {
    }

    if (type == String.class) {
      if (value == null || value instanceof String) {
        return value;
      }
      return value.toString();
    }

    if (type == byte[].class && value instanceof String string) {
      return string.getBytes();
    }

    if (type == Character.TYPE || type == Character.class)
      return Character.valueOf(value.toString().charAt(0));

    if (type == Byte.TYPE || type == Byte.class) return Byte.valueOf(value.toString());

    if (type == Short.TYPE || type == Short.class) return Short.valueOf(value.toString());

    if (type == Integer.TYPE || type == Integer.class) {
      if (value instanceof Number number) {
        return number.intValue();
      }
      return Integer.valueOf(value.toString());
    }

    if (type == Long.TYPE || type == Long.class) return Long.valueOf(value.toString());

    if (type == Float.TYPE || type == Float.class) return Float.valueOf(value.toString());

    if (type == Date.class) return Date.valueOf(value.toString());

    if (type == Time.class) return Time.valueOf(value.toString());

    if (type == Timestamp.class) return Timestamp.valueOf(value.toString());

    return value;
  }

  public Object adaptNull(Object value, Class<?> type, Type genericType, Annotation[] annotations) {

    if (isNullable(type, annotations)) return null;

    if (type == boolean.class) return false;

    if (type == int.class) return 0;

    if (type == long.class) return 0L;

    if (type == double.class) return 0.0;

    if (type == short.class) return 0.0F;

    if (type == char.class) return ' ';

    return null;
  }

  private boolean isNullable(Class<?> type, Annotation[] annotations) {
    if (type.isPrimitive()) {
      return false;
    }
    if (annotations == null || annotations.length == 0) {
      return true;
    }

    for (Annotation annotation : annotations) {
      if (annotation instanceof Column column) {
        return column.nullable();
      }
    }

    return false;
  }
}
