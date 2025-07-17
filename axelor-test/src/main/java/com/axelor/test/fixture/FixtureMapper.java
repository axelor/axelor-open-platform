/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.test.fixture;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

class FixtureMapper {

  private final Map<Class<?>, Map<String, Object>> records = new HashMap<>();

  public final <T> T map(Class<T> type, String key, Map<?, ?> data) {
    try {
      var info = Introspector.getBeanInfo(type);
      var bean = findBean(type, key);

      if (data == null || data.isEmpty()) {
        return bean;
      }

      // Set properties
      for (var property : info.getPropertyDescriptors()) {
        var name = property.getName();
        var method = property.getWriteMethod();
        if (method != null && data.containsKey(name)) {
          var value = accept(property, data.get(name));
          method.setAccessible(true);
          method.invoke(bean, value);
        }
      }
      return type.cast(bean);
    } catch (Exception e) {
      throw new RuntimeException("Failed to map bean of type: " + type.getName(), e);
    }
  }

  private Object accept(PropertyDescriptor property, Object value) {
    if (value instanceof Iterable<?> items) {
      var list =
          Set.class.isAssignableFrom(property.getPropertyType())
              ? new HashSet<>()
              : new ArrayList<>();
      for (Object item : items) {
        item = acceptValue(property, item);
        list.add(item);
      }
      return list;
    }
    return acceptValue(property, value);
  }

  private Object acceptValue(PropertyDescriptor property, Object value) {
    if (value == null) return null;
    var type = findType(property);
    if (type.isInstance(value)) {
      return value;
    }

    // Handle enum
    if (type.isEnum()) {
      return acceptEnum(value, type);
    }
    // Handle number
    if (Number.class.isAssignableFrom(type)) {
      return acceptNumber(value, type);
    }
    // Handle Java Time types
    if (Temporal.class.isAssignableFrom(type)) {
      return acceptTemporal(value, type.asSubclass(Temporal.class));
    }

    // Handle simple type
    if (type == String.class) return value.toString();
    if (type == Character.TYPE || type == Character.class) return value.toString().charAt(0);
    if (type == byte[].class && value instanceof String string) return string.getBytes();
    if (type == Byte.TYPE || type == Byte.class) return Byte.valueOf(value.toString());
    if (Boolean.class.isAssignableFrom(type)) {
      if (value instanceof Boolean) return value;
      if (value instanceof String) {
        return Boolean.parseBoolean((String) value);
      }
      throw new IllegalArgumentException("Cannot convert value to boolean: " + value);
    }

    // Handle references
    if (records.containsKey(type)) {
      return acceptReference(type, value.toString());
    }

    throw new IllegalArgumentException("Cannot convert value to " + type.getName() + ": " + value);
  }

  private Object acceptNumber(Object value, Class<?> type) {
    if (type == Short.TYPE || type == Short.class) return Short.valueOf(value.toString());
    if (type == Integer.TYPE || type == Integer.class) {
      if (value instanceof Number number) {
        return number.intValue();
      }
      return Integer.valueOf(value.toString());
    }
    if (type == Long.TYPE || type == Long.class) return Long.valueOf(value.toString());
    if (type == Float.TYPE || type == Float.class) return Float.valueOf(value.toString());
    if (BigDecimal.class.isAssignableFrom(type)) return new BigDecimal(value.toString());

    try {
      return type.getConstructor(String.class).newInstance(value.toString());
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Cannot convert value to " + type.getName() + ": " + value, e);
    }
  }

  private Object acceptEnum(Object value, Class<?> type) {
    return Stream.of(type.getEnumConstants())
        .filter(e -> e.toString().equals(value.toString()))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Invalid enum value: " + value + " for type: " + type.getName()));
  }

  private Object acceptTemporal(Object value, Class<?> type) {
    if (value instanceof Date date) {
      var zoned = date.toInstant().atZone(ZoneOffset.UTC);
      if (type == LocalDate.class) {
        return zoned.toLocalDate();
      } else if (type == LocalDateTime.class) {
        return zoned.toLocalDateTime();
      } else if (type == LocalTime.class) {
        return zoned.toLocalTime();
      } else if (type == ZonedDateTime.class) {
        return zoned;
      } else {
        throw new IllegalArgumentException("Unsupported temporal type: " + type.getName());
      }
    }

    if (value instanceof String str && type == LocalTime.class) {
      try {
        return LocalTime.parse(str);
      } catch (Exception e) {
        throw new IllegalArgumentException("Cannot parse LocalTime from string: " + str, e);
      }
    }

    throw new IllegalArgumentException(
        "Unsupported temporal type: " + type.getName() + " for value: " + value);
  }

  private Object acceptReference(Class<?> type, String key) {
    var refs = records.getOrDefault(type, Collections.emptyMap());
    if (refs.containsKey(key)) {
      return refs.get(key);
    }
    throw new IllegalArgumentException(
        "No record found for type: " + type.getName() + " with key: " + key);
  }

  private <T> T findBean(Class<T> type, String key) {
    var beans = records.getOrDefault(type, Collections.emptyMap());
    if (beans.containsKey(key)) {
      return type.cast(beans.get(key));
    }
    try {
      var ctor = type.getDeclaredConstructor();
      // Ensure the constructor is accessible
      ctor.setAccessible(true);
      var bean = ctor.newInstance();
      records.computeIfAbsent(type, k -> new HashMap<>()).put(key, bean);
      return type.cast(bean);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create bean of type: " + type.getName(), e);
    }
  }

  private Class<?> findType(PropertyDescriptor prop) {
    var type = prop.getPropertyType();
    if (Collection.class.isAssignableFrom(type)) {
      var genericType = prop.getReadMethod().getGenericReturnType();
      if (genericType instanceof ParameterizedType) {
        var paramType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
        if (paramType instanceof Class<?>) {
          type = (Class<?>) paramType;
        } else {
          throw new IllegalArgumentException("Unsupported generic type: " + paramType);
        }
      }
    }
    return type;
  }
}
