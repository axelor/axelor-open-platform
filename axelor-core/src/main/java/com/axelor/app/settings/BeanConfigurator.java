/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.app.settings;

import com.axelor.common.Inflector;
import com.axelor.inject.Beans;
import jakarta.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Bean configurator using reflection
 *
 * <p>This provides utility methods for dynamically accessing and modifying bean properties using
 * reflection. It can convert values that typically come from configuration files to the appropriate
 * types for property assignment.
 */
public class BeanConfigurator {

  private static final Map<Class<?>, UnaryOperator<Object>> converters =
      new HashMap<>(
          Map.ofEntries(
              Map.entry(
                  URL.class,
                  value -> {
                    // Use {@link java.net.URI#toURL()} to construct an instance of URL,
                    // because java.net.URL(String) constructor is deprecated.
                    try {
                      return new URI(String.valueOf(value)).toURL();
                    } catch (MalformedURLException | URISyntaxException e) {
                      throw new RuntimeException(e);
                    }
                  })));

  private static final Map<Class<?>, Class<?>> PRIMITIVE_TYPES =
      Map.ofEntries(
          Map.entry(byte.class, Byte.class),
          Map.entry(short.class, Short.class),
          Map.entry(int.class, Integer.class),
          Map.entry(long.class, Long.class),
          Map.entry(float.class, Float.class),
          Map.entry(double.class, Double.class),
          Map.entry(boolean.class, Boolean.class),
          Map.entry(char.class, Character.class));

  private BeanConfigurator() {}

  /**
   * Gets a property value from a bean.
   *
   * @param obj the bean
   * @param property the property name
   * @return the property value
   */
  public static Object getField(Object obj, String property) {
    try {
      return getFieldChecked(obj, property);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets a property value from a bean.
   *
   * <p>This can throw a checked {@link ReflectiveOperationException}.
   *
   * @param obj the bean
   * @param property the property name
   * @return the property value
   * @throws NoSuchMethodException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   */
  public static Object getFieldChecked(Object obj, String property)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    return findGetter(obj.getClass(), property).invoke(obj);
  }

  /**
   * Sets a property value on a bean.
   *
   * @param obj the bean
   * @param property the property name
   * @param value the property value
   */
  public static void setField(Object obj, String property, Object value) {
    try {
      setFieldChecked(obj, property, value);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sets a property value on a bean.
   *
   * <p>This can throw a checked {@link ReflectiveOperationException}.
   *
   * @param obj the bean
   * @param property the property name
   * @param value the property value
   * @throws NoSuchMethodException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   */
  public static void setFieldChecked(Object obj, String property, Object value)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    final Method setter = findSetter(obj.getClass(), property);
    Class<?> type = setter.getParameterTypes()[0];
    type = PRIMITIVE_TYPES.getOrDefault(type, type);
    final Type genericType = setter.getGenericParameterTypes()[0];
    final Object converted = convert(value, type, genericType);
    setter.invoke(obj, converted);
  }

  /**
   * Finds a getter method for a property.
   *
   * @param klass the class to search
   * @param property the property name
   * @return the getter method
   * @throws NoSuchMethodException
   */
  public static Method findGetter(Class<?> klass, String property) throws NoSuchMethodException {
    final String getter = "get" + Inflector.getInstance().camelize(property);
    final List<Method> getterMethods =
        Stream.of(klass.getMethods())
            .filter(m -> m.getName().equalsIgnoreCase(getter))
            .filter(m -> m.getParameterCount() == 0)
            .toList();

    if (getterMethods.isEmpty()) {
      throw new NoSuchMethodException(String.format("%s.%s", klass.getName(), getter));
    }

    Method result = getterMethods.get(0);
    for (final Method method : getterMethods.subList(1, getterMethods.size())) {
      if (result.getReturnType().isAssignableFrom(method.getReturnType())) {
        result = method;
      }
    }

    return result;
  }

  /**
   * Finds a setter method for a property.
   *
   * @param klass the class to search
   * @param property the property name
   * @return the setter method
   * @throws NoSuchMethodException
   */
  public static Method findSetter(Class<?> klass, String property) throws NoSuchMethodException {
    final String setter = "set" + Inflector.getInstance().camelize(property);
    return Stream.of(klass.getMethods())
        .filter(m -> m.getName().equalsIgnoreCase(setter))
        .filter(m -> m.getParameterCount() == 1)
        .findAny()
        .orElseThrow(
            () -> new NoSuchMethodException(String.format("%s.%s", klass.getName(), setter)));
  }

  public static void registerConverter(Class<?> type, UnaryOperator<Object> converter) {
    converters.put(type, converter);
  }

  /**
   * Converts a value to the given type.
   *
   * @param value the value
   * @param type the type
   * @param genericType the generic type
   * @return the converted value
   */
  private static Object convert(Object value, Class<?> type, Type genericType) {
    if (value == null) {
      return null;
    }

    final UnaryOperator<Object> converter = converters.get(type);
    if (converter != null) {
      return converter.apply(value);
    }

    final String valueStr = String.valueOf(value);

    // Convert to collection if required
    if (Collection.class.isAssignableFrom(type) && !(value instanceof Collection)) {
      value = convertToCollection(valueStr, type, genericType);
    }

    // Already the right type
    if (type.isAssignableFrom(value.getClass())) {
      return value;
    }

    // Try common parse methods (eg. `Integer.valueOf(string)`)
    Object result = tryParseMethods(type, valueStr);
    if (result != null) {
      return result;
    }

    // Try direct constructor with value (eg. `new URL(string)`)
    result = tryConstructor(type, value);
    if (result != null) {
      return result;
    }

    // Try string constructor if value isn't already a string
    if (!(value instanceof String)) {
      result = tryConstructor(type, valueStr);
      if (result != null) {
        return result;
      }
    }

    // Fallback to class instantiation (eg. `Beans.get(Class.forName(string))`)
    try {
      return getInstance(findClass(valueStr));
    } catch (Exception e) {
      throw new IllegalArgumentException(String.format("Can't convert \"%s\" to %s", value, type));
    }
  }

  // Try injection and fall back to constructor in case Guice is not initialized
  private static Object getInstance(Class<?> klass) throws ReflectiveOperationException {
    try {
      return Beans.get(klass);
    } catch (Exception e) {
      return klass.getConstructor().newInstance();
    }
  }

  private static Object convertToCollection(String value, Class<?> type, Type genericType) {
    List<?> items = Arrays.asList(value.split("\\s*,\\s*"));

    if (genericType instanceof ParameterizedType parameterizedType) {
      final Class<?> itemType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
      items = items.stream().map(item -> convert(item, itemType, null)).toList();
    }

    return type.isAssignableFrom(Set.class) ? new HashSet<>(items) : items;
  }

  @Nullable
  private static Object tryConstructor(Class<?> type, Object param) {
    try {
      return type.getConstructor(param.getClass()).newInstance(param);
    } catch (NoSuchMethodException e) {
      return null;
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  private static Object tryParseMethods(Class<?> type, String value) {
    return Stream.of("valueOf", "parse")
        .map(
            name -> {
              try {
                Method method = type.getMethod(name, String.class);
                return method.invoke(null, value);
              } catch (NoSuchMethodException e) {
                return null;
              } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
              }
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private static Class<?> findClass(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
