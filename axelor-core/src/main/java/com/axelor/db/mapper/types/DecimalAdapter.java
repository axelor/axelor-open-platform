/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.mapper.types;

import com.axelor.db.mapper.TypeAdapter;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Digits;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class DecimalAdapter implements TypeAdapter<BigDecimal> {

  @Override
  public Object adapt(
      Object value, Class<?> actualType, Type genericType, Annotation[] annotations) {

    Integer scale = null;
    Boolean nullable = null;
    for (Annotation a : annotations) {
      if (a instanceof Digits digits) {
        scale = digits.fraction();
      }
      if (a instanceof Column column) {
        nullable = column.nullable();
      }
    }

    boolean empty = value == null || (value instanceof String s && "".equals(s.trim()));
    if (empty) {
      return Boolean.TRUE.equals(nullable) ? null : BigDecimal.ZERO;
    }

    if (value instanceof BigDecimal decimal) {
      return adjust(decimal, scale);
    }
    return adjust(new BigDecimal(value.toString()), scale);
  }

  private BigDecimal adjust(BigDecimal value, Integer scale) {
    if (scale != null) {
      return value.setScale(scale, RoundingMode.HALF_UP);
    }
    return value;
  }
}
