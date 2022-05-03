/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.rpc;

import com.axelor.common.StringUtils;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.i18n.I18n;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

final class Translator {

  private Translator() {}

  private static String getTranslation(String value) {
    if (StringUtils.notBlank(value)) {
      String key = toValueKey(value);
      String val = I18n.get(key);
      if (!Objects.equals(val, key)) {
        return val;
      }
    }
    return value;
  }

  private static String toValueKey(String name) {
    return "value:" + name;
  }

  private static String toKey(String name) {
    return "$t:" + name;
  }

  @Nullable
  private static Property getProperty(Mapper mapper, String field) {
    Property property = null;
    Iterator<String> names = Arrays.stream(field.split("\\.")).iterator();
    while (names.hasNext()) {
      property = mapper.getProperty(names.next());
      if (property == null) return null;
      if (names.hasNext()) {
        if (property.getTarget() == null) return null;
        mapper = Mapper.of(property.getTarget());
      }
    }
    return property;
  }

  static Map<String, Object> translate(Map<String, Object> values, Property property) {
    return translate(values, property.getName());
  }

  static Map<String, Object> translate(Map<String, Object> values, String name) {
    Object value = values.get(name);
    if (value instanceof String) {
      Object val = getTranslation((String) value);
      if (!Objects.equals(val, value)) {
        values.put(toKey(name), val);
      }
    }
    return values;
  }

  static void applyTranslatables(Map<String, Object> values, Class<?> model) {
    final Mapper mapper = Mapper.of(model);
    final Collection<String> names = values.keySet().stream().collect(Collectors.toList());

    names.forEach(
        name -> {
          final Object value = values.get(name);
          if (value instanceof String) {
            final Property property = getProperty(mapper, name);
            if (property != null && property.isTranslatable()) {
              translate(values, name);
            }
          } else if (value instanceof Map) {
            final Property property = getProperty(mapper, name);
            if (property != null && property.getTarget() != null) {
              @SuppressWarnings("unchecked")
              final Map<String, Object> map = (Map<String, Object>) value;
              applyTranslatables(map, property.getTarget());
            }
          }
        });
  }
}
