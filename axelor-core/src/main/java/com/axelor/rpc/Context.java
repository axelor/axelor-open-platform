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
package com.axelor.rpc;

import com.axelor.common.StringUtils;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.MetaStore;
import com.axelor.meta.db.MetaJsonRecord;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.script.SimpleBindings;

/**
 * The Context class represents an {@link ActionRequest} context.
 *
 * <p>The request context is mapped to a proxy instance of the bean class on which the action is
 * being performed. The proxy instance can be accessed via {@link #asType(Class)} method.
 *
 * <p>Example (Java):
 *
 * <pre>
 * Context context = request.getContext();
 * SaleOrderLine soLine = context.asType(SaleOrderLine.class);
 * SaleOrder so = context.getParentContext().asType(SaleOrder.class);
 * </pre>
 *
 * Example (Groovy):
 *
 * <pre>
 * def context = request.context
 * def soLine = context as SaleOrderLine
 * def so = context.parentContext as SaleOrder
 * </pre>
 *
 * The instance returned from the context is a detached proxy object and should not be used with
 * JPA/Hibernate session. It's only for convenience to get the context values using the bean
 * methods.
 */
@JsonSerialize(using = Context.Serializer.class)
public class Context extends SimpleBindings {

  static class Serializer extends JsonSerializer<Context> {

    @Override
    public void serialize(Context value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonProcessingException {
      if (value != null) {
        final JsonSerializer<Object> serializer = provider.findValueSerializer(Map.class, null);
        final Map<String, Object> map = new HashMap<>();
        value
            .entrySet()
            .stream()
            .filter(e -> !(e.getValue() instanceof JsonContext))
            .filter(e -> !KEY_PARENT_CONTEXT.equals(e.getKey()))
            .forEach(e -> map.put(e.getKey(), e.getValue()));
        serializer.serialize(map, jgen, provider);
      }
    }
  }

  private static final String KEY_MODEL = "_model";
  private static final String KEY_PARENT = "_parent";
  private static final String KEY_PARENT_CONTEXT = "parentContext";

  static final String KEY_ID = "id";
  static final String KEY_JSON_ATTRS = "attrs";
  static final String KEY_JSON_MODEL = "jsonModel";
  static final String KEY_JSON_PREFIX = "$";

  private final Map<String, Object> values;

  private final Mapper mapper;

  private final Class<?> beanClass;

  private ContextHandler<?> handler;

  private Context parent;

  private Map<String, Object> jsonFields;

  /**
   * Create a new {@link Context} for the given bean class using the given context values.
   *
   * @param values the context values
   * @param beanClass the context bean class
   */
  public Context(Map<String, Object> values, Class<?> beanClass) {
    super(values);
    this.values = Objects.requireNonNull(values);
    this.beanClass = Objects.requireNonNull(beanClass);
    this.mapper = Mapper.of(beanClass);
  }

  /**
   * Create a new {@link Context} for the given bean class.
   *
   * @param beanClass the context bean class
   */
  public Context(Class<?> beanClass) {
    this(new HashMap<>(), beanClass);
  }

  /**
   * Create a new {@link Context} for a record by given id.
   *
   * <p>This is useful when we have to use custom fields defined for an entity from bussiness code.
   *
   * @param id the record id
   * @param beanClass the record entity class
   */
  public Context(Long id, Class<?> beanClass) {
    this(beanClass);
    this.values.put("id", id);
  }

  public void addChangeListener(PropertyChangeListener listener) {
    getContextHandler().addChangeListener(listener);
  }

  private Map<String, Object> jsonFields() {
    if (jsonFields == null) {
      jsonFields =
          MetaJsonRecord.class.isAssignableFrom(beanClass)
              ? MetaStore.findJsonFields((String) values.get(KEY_JSON_MODEL))
              : MetaStore.findJsonFields(beanClass.getName(), KEY_JSON_ATTRS);
    }
    return jsonFields;
  }

  private ContextHandler<?> getContextHandler() {
    if (handler == null) {
      handler = ContextHandlerFactory.newHandler(beanClass, values);
    }
    return handler;
  }

  private Object getProxy() {
    return getContextHandler().getProxy();
  }

  /**
   * Get parent context.
   *
   * @return the parent context if exist
   */
  @SuppressWarnings("unchecked")
  public Context getParent() {
    if (parent != null) {
      return parent;
    }
    final Object value = values.get(KEY_PARENT);
    if (value == null) {
      return null;
    }
    try {
      Map<String, Object> valueMap = (Map<String, Object>) value;
      Class<?> parentClass = Class.forName((String) valueMap.get(KEY_MODEL));
      parent = new Context(valueMap, parentClass);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return parent;
  }

  /** @see #getParent() */
  @Deprecated
  public Context getParentContext() {
    return getParent();
  }

  public Class<?> getContextClass() {
    return beanClass;
  }

  /**
   * Convert the context as lazy entity of the given type.
   *
   * @param type the expected type
   * @return lazy initialized entity
   */
  @SuppressWarnings("unchecked")
  public <T> T asType(Class<T> type) {
    final T bean = (T) getProxy();
    if (!type.isInstance(bean)) {
      throw new IllegalArgumentException(
          String.format("Invalid type {}, should be {}", type.getName(), beanClass.getName()));
    }
    return bean;
  }

  private String checkKey(Object key) {
    if (key == null) {
      throw new NullPointerException("key can not be null");
    }
    if (!(key instanceof String)) {
      throw new ClassCastException("key should be a String");
    }
    if (StringUtils.isEmpty((String) key)) {
      throw new IllegalArgumentException("key can not be empty");
    }
    return (String) key;
  }

  private boolean isJsonName(String name) {
    return isJsonRecord() && "name".equals(name);
  }

  private boolean isJsonRecord() {
    return MetaJsonRecord.class.isAssignableFrom(getContextClass());
  }

  private boolean hasJsonField(String name) {
    return !KEY_JSON_MODEL.equals(name)
        && !KEY_JSON_ATTRS.equals(name)
        && jsonFields() != null
        && jsonFields().containsKey(name);
  }

  private boolean isJsonField(String name) {
    return name.startsWith(KEY_JSON_PREFIX)
        && mapper.getProperty(name.substring(KEY_JSON_PREFIX.length())) != null
        && mapper.getProperty(name.substring(KEY_JSON_PREFIX.length())).isJson();
  }

  private JsonContext getJsonContext(Property property, Object value) {
    final String name = property.getName();
    return (JsonContext)
        values.computeIfAbsent(
            KEY_JSON_PREFIX + name, k -> new JsonContext(this, property, (String) value));
  }

  private JsonContext getJsonContext() {
    final Property property = mapper.getProperty(KEY_JSON_ATTRS);
    return getJsonContext(property, property.get(getProxy()));
  }

  private Object tryJsonGet(String name) {
    if (hasJsonField(name)) {
      return getJsonContext().get(name);
    }
    return super.get(name);
  }

  private Object tryJsonPut(String name, Object value) {
    if (hasJsonField(name)) {
      return getJsonContext().put(name, value);
    }
    return super.put(name, value);
  }

  @Override
  public boolean containsKey(Object key) {
    final String name = checkKey(key);
    return super.containsKey(name)
        || KEY_PARENT_CONTEXT.equals(name)
        || isJsonField(name)
        || hasJsonField(name)
        || mapper.getProperty((String) key) != null;
  }

  @Override
  public Object get(Object key) {
    checkKey(key);
    if (KEY_PARENT_CONTEXT.equals(key)) {
      return getParent();
    }
    final String name = (String) key;
    final Object value = super.get(name);

    // if cached json context
    if (value instanceof JsonContext) {
      return value;
    }

    final Property property = mapper.getProperty(name);

    // if real field access
    if (property != null && !isJsonName(name)) {
      return property.get(getProxy());
    }

    // try json context
    if (isJsonField(name)) {
      final Property jsonProperty = mapper.getProperty(name.substring(KEY_JSON_PREFIX.length()));
      return getJsonContext(jsonProperty, jsonProperty.get(getProxy()));
    }

    // else try json fields
    return tryJsonGet((String) key);
  }

  @Override
  public Object put(String name, Object value) {
    if (mapper.getSetter(name) == null || (isJsonRecord() && hasJsonField(name))) {
      if (isJsonName(name)) {
        mapper.set(getProxy(), name, value);
      }
      return tryJsonPut(name, value);
    }
    return mapper.set(getProxy(), name, handler.validate(mapper.getProperty(name), value));
  }
}
