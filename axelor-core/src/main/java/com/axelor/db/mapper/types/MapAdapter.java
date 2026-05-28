/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.mapper.types;

import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.TypeAdapter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

public class MapAdapter implements TypeAdapter<Map<?, ?>> {

  static boolean isModelMap(Class<?> type, Object value) {
    return value instanceof Map
        && Model.class.isAssignableFrom(type)
        && !Model.class.isInstance(value);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object adapt(Object value, Class<?> type, Type genericType, Annotation[] annotations) {
    if (isModelMap(type, value)) {
      return Mapper.toBean(type.asSubclass(Model.class), (Map<String, Object>) value);
    }
    return value;
  }
}
