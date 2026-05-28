/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.mapper.types;

import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.TypeAdapter;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SetAdapter implements TypeAdapter<Set<?>> {

  @SuppressWarnings("unchecked")
  @Override
  public Object adapt(Object value, Class<?> type, Type genericType, Annotation[] annotations) {
    final Class<?> fieldType =
        (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];
    final Set<Object> val = new HashSet<>();
    for (Object obj : (Collection<?>) value) {
      if (MapAdapter.isModelMap(fieldType, obj)) {
        val.add(Mapper.toBean(fieldType.asSubclass(Model.class), (Map<String, Object>) obj));
      } else {
        val.add(obj);
      }
    }
    return val;
  }
}
