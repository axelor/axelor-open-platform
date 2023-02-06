/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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

import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.meta.MetaStore;
import com.axelor.meta.db.MetaJsonRecord;
import com.axelor.rpc.Context;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;

public class JsonProperty extends Property {

  final Property property;
  final Map<String, Object> jsonField;
  final String fieldName;
  final String subFieldName;

  public static final String KEY_JSON_PREFIX = "$";
  private static final Annotation[] EMPTY_ANNOTATIONS = {};

  private JsonProperty(
      Property property, Map<String, Object> jsonField, String fieldName, String subFieldName) {
    super(
        property.getEntity(),
        property.getName(),
        property.getJavaType(),
        property.getGenericType(),
        EMPTY_ANNOTATIONS);

    this.property = property;
    this.jsonField = jsonField;
    this.fieldName = fieldName;
    this.subFieldName = subFieldName;
  }

  @Nullable
  public static JsonProperty of(Class<?> beanClass, String field) {
    return of(beanClass, null, field);
  }

  @Nullable
  public static JsonProperty of(String jsonModel, String field) {
    return of(MetaJsonRecord.class, jsonModel, field);
  }

  @Nullable
  private static JsonProperty of(Class<?> beanClass, String jsonModel, String field) {
    if (StringUtils.isBlank(field)) {
      return null;
    }

    final List<String> fieldParts = Arrays.asList(field.split("\\.", 2));

    if (fieldParts.size() < 2) {
      return null;
    }

    final String fieldName = fieldParts.get(0);
    final String subFieldName = fieldParts.get(1);
    final String propertyName =
        fieldName.startsWith(KEY_JSON_PREFIX)
            ? fieldName.substring(KEY_JSON_PREFIX.length())
            : fieldName;
    final Map<String, Object> jsonFields =
        Optional.ofNullable(
                jsonModel != null
                    ? MetaStore.findJsonFields(jsonModel)
                    : MetaStore.findJsonFields(beanClass.getName(), propertyName))
            .orElse(Collections.emptyMap());

    @SuppressWarnings("unchecked")
    final Map<String, Object> jsonField = (Map<String, Object>) jsonFields.get(subFieldName);

    if (jsonField == null) {
      return null;
    }

    final Mapper mapper = Mapper.of(beanClass);
    final Property property = mapper.getProperty(propertyName);

    return new JsonProperty(property, jsonField, fieldName, subFieldName);
  }

  @Override
  public PropertyType getType() {
    return PropertyType.get(getJsonType().replace("-", "_").toUpperCase());
  }

  @Override
  public Class<?> getTarget() {
    if (!isReference() && !isCollection()) {
      return null;
    }

    return Optional.ofNullable(
            (String) jsonField.get(getJsonType().startsWith("json-") ? "jsonTarget" : "target"))
        .map(
            target -> {
              try {
                return Class.forName(target);
              } catch (ClassNotFoundException e) {
                return null;
              }
            })
        .orElse(null);
  }

  @Override
  public boolean isReference() {
    return getJsonType().endsWith("-to-one");
  }

  @Override
  public boolean isCollection() {
    return getJsonType().endsWith("-to-many");
  }

  @Override
  public Object get(Object bean) {
    return getBindings(getContext(bean)).get(subFieldName);
  }

  @Override
  public Object set(Object bean, Object value) {
    final Context context = getContext(bean);
    final Map<String, Object> bindings = getBindings(context);
    bindings.put(subFieldName, persist(value));
    return property.set(bean, context.get(property.getName()));
  }

  @Override
  public Object add(Object bean, Object item) {
    return add(bean, collection -> collection.add(item));
  }

  @Override
  public Object addAll(Object bean, Collection<?> items) {
    return add(bean, collection -> collection.addAll(items));
  }

  private Object add(Object bean, Predicate<Collection<Object>> adder) {
    final Context context = getContext(bean);
    final Map<String, Object> bindings = getBindings(context);

    @SuppressWarnings("unchecked")
    final Collection<Object> items =
        Optional.ofNullable((Collection<Object>) bindings.get(subFieldName))
            .orElseGet(ArrayList::new);

    if (adder.test(items)) {
      bindings.put(subFieldName, persist(items));
      property.set(bean, context.get(property.getName()));
    }

    return bean;
  }

  private Object persist(Object value) {
    if (value instanceof Model) {
      final Model bean = (Model) value;
      if (Optional.ofNullable(bean.getId()).orElse(0L) <= 0L) {
        JPA.em().persist(bean);
      }
    } else if (value instanceof Collection) {
      ((Collection<?>) value).forEach(this::persist);
    }
    return value;
  }

  private Context getContext(Object bean) {
    return new Context(Mapper.toMap(bean), bean.getClass());
  }

  private Map<String, Object> getBindings(Context context) {
    final Object item = context.get(fieldName);

    if (item instanceof Map) {
      @SuppressWarnings("unchecked")
      final Map<String, Object> bindings = (Map<String, Object>) item;
      return bindings;
    }

    return Collections.emptyMap();
  }

  private String getJsonType() {
    return (String) jsonField.getOrDefault("type", "");
  }

  @Override
  public String getName() {
    return String.format("%s.%s", fieldName, subFieldName);
  }

  @Override
  public boolean isPrimary() {
    return false;
  }

  @Override
  public boolean isVirtual() {
    return false;
  }
}
