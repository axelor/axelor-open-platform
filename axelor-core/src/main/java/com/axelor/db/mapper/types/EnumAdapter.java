/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.mapper.types;

import com.axelor.db.ValueEnum;
import com.axelor.db.mapper.TypeAdapter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class EnumAdapter implements TypeAdapter<Enum<?>> {

  @Override
  @SuppressWarnings("unchecked")
  public Object adapt(
      Object value, Class<?> actualType, Type genericType, Annotation[] annotations) {
    if (value == null) {
      return null;
    }
    if (!actualType.isEnum()) {
      throw new IllegalArgumentException("Given type is not enum: " + actualType.getName());
    }
    if (value instanceof Enum) {
      return value;
    }
    return ValueEnum.class.isAssignableFrom(actualType)
        ? ValueEnum.of(actualType.asSubclass(Enum.class), value)
        : Enum.valueOf(actualType.asSubclass(Enum.class), value.toString());
  }
}
