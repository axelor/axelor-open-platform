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
package com.axelor.db.mapper.types;

import com.axelor.db.mapper.TypeAdapter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.persistence.Column;
import javax.validation.constraints.Digits;

public class DecimalAdapter implements TypeAdapter<BigDecimal> {

  @Override
  public Object adapt(
      Object value, Class<?> actualType, Type genericType, Annotation[] annotations) {

    Integer scale = null;
    Boolean nullable = null;
    for (Annotation a : annotations) {
      if (a instanceof Digits) {
        scale = ((Digits) a).fraction();
      }
      if (a instanceof Column) {
        nullable = ((Column) a).nullable();
      }
    }

    boolean empty =
        value == null || (value instanceof String && "".equals(((String) value).trim()));
    if (empty) {
      return nullable == Boolean.TRUE ? null : BigDecimal.ZERO;
    }

    if (value instanceof BigDecimal) {
      return adjust((BigDecimal) value, scale);
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
