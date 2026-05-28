/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import static com.axelor.common.StringUtils.isBlank;

import com.axelor.app.AppSettings;
import com.axelor.inject.Beans;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
class ConfigContext extends HashMap<String, Object> {
  private static Map<String, String> CONFIG_CONTEXT;

  private Map<String, Object> values = new HashMap<>();

  public ConfigContext() {
    if (CONFIG_CONTEXT == null) {
      CONFIG_CONTEXT = new HashMap<>();
      final Map<String, String> ctxProperties =
          AppSettings.get().getPropertiesStartingWith("context.");
      for (final Entry<String, String> entry : ctxProperties.entrySet()) {
        final String name = entry.getKey();
        final String expr = entry.getValue();
        if (isBlank(expr)) {
          continue;
        }
        CONFIG_CONTEXT.put(name.substring(8), expr);
      }
    }
  }

  @Override
  public Set<String> keySet() {
    return CONFIG_CONTEXT.keySet();
  }

  @Override
  public boolean containsKey(Object key) {
    return CONFIG_CONTEXT.containsKey(key);
  }

  @Override
  public Object get(Object key) {
    if (values.containsKey(key) || !containsKey(key)) {
      return values.get(key);
    }

    final String name = (String) key;
    final String expr = CONFIG_CONTEXT.get(key);
    final String[] parts = expr.split("\\:", 2);
    final Object invalid = new Object();

    Class<?> klass = null;
    Object value = invalid;

    try {
      klass = Class.forName(parts[0]);
    } catch (ClassNotFoundException e) {
    }

    if (klass == null) {
      value = adapt(expr);
      values.put(name, value);
      return value;
    }

    try {
      value = klass.getField(parts[1]).get(null);
    } catch (Exception e) {
    }
    try {
      value = klass.getMethod(parts[1]).invoke(null);
    } catch (Exception e) {
    }

    if (value != invalid) {
      values.put(name, value);
      return value;
    }

    final Object instance = Beans.get(klass);

    if (parts.length == 1) {
      value = instance;
      values.put(name, value);
      return value;
    }

    try {
      value = klass.getMethod(parts[1]).invoke(instance);
    } catch (Exception e) {
    }

    if (value == invalid) {
      throw new RuntimeException("Invalid configuration: " + name + " = " + expr);
    }

    values.put(name, value);
    return value;
  }

  private Object adapt(String value) {
    if (isBlank(value)) {
      return null;
    }
    if ("true".equals(value.toLowerCase())) {
      return true;
    }
    if ("false".equals(value.toLowerCase())) {
      return false;
    }
    try {
      return Integer.parseInt(value);
    } catch (Exception e) {
    }
    return value;
  }
}
