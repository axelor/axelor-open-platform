/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
package com.axelor.db.mapper;

import com.axelor.db.mapper.types.DecimalAdapter;
import com.axelor.db.mapper.types.EnumAdapter;
import com.axelor.db.mapper.types.JavaTimeAdapter;
import com.axelor.db.mapper.types.ListAdapter;
import com.axelor.db.mapper.types.MapAdapter;
import com.axelor.db.mapper.types.SetAdapter;
import com.axelor.db.mapper.types.SimpleAdapter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Adapter {

  private static SimpleAdapter simpleAdapter = new SimpleAdapter();
  private static ListAdapter listAdapter = new ListAdapter();
  private static SetAdapter setAdapter = new SetAdapter();
  private static MapAdapter mapAdapter = new MapAdapter();
  private static JavaTimeAdapter javaTimeAdapter = new JavaTimeAdapter();
  private static EnumAdapter enumAdapter = new EnumAdapter();

  private static DecimalAdapter decimalAdapter = new DecimalAdapter();

  public static Object adapt(
      Object value, Class<?> type, Type genericType, Annotation[] annotations) {

    if (annotations == null) {
      annotations = new Annotation[] {};
    }

    if (type.isEnum()) {
      return enumAdapter.adapt(value, type, genericType, annotations);
    }

    // one2many
    if (value instanceof Collection && List.class.isAssignableFrom(type)) {
      return listAdapter.adapt(value, type, genericType, annotations);
    }

    // many2many
    if (value instanceof Collection && Set.class.isAssignableFrom(type)) {
      return setAdapter.adapt(value, type, genericType, annotations);
    }

    // many2one
    if (value instanceof Map) {
      return mapAdapter.adapt(value, type, genericType, annotations);
    }

    // collection of simple types
    if (value instanceof Collection) {
      Collection<Object> all = value instanceof Set ? new HashSet<>() : new ArrayList<>();
      for (Object item : (Collection<?>) value) {
        all.add(adapt(item, type, genericType, annotations));
      }
      return all;
    }

    // must be after adapting collections
    if (type.isInstance(value)) {
      return value;
    }

    if (javaTimeAdapter.isJavaTimeObject(type)) {
      return javaTimeAdapter.adapt(value, type, genericType, annotations);
    }

    if (BigDecimal.class.isAssignableFrom(type)) {
      return decimalAdapter.adapt(value, type, genericType, annotations);
    }

    return simpleAdapter.adapt(value, type, genericType, annotations);
  }
}
