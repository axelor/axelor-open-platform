/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.app;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentVariablesExtension implements BeforeAllCallback, AfterAllCallback {

  private static final Logger LOG = LoggerFactory.getLogger(EnvironmentVariablesExtension.class);

  protected final Set<String> modified = new HashSet<>();

  public EnvironmentVariablesExtension() {}

  @Override
  public void afterAll(ExtensionContext context) {
    clearAll();
  }

  @Override
  public void beforeAll(ExtensionContext context) {}

  public EnvironmentVariablesExtension set(String name, String value) {
    writeVariableToEnvMap(name, value);
    return this;
  }

  public void clearAll() {
    for (String name : modified) {
      writeVariableToEnvMap(name, null);
    }
  }

  public EnvironmentVariablesExtension clear(String... names) {
    for (String name : names) {
      writeVariableToEnvMap(name, null);
    }
    return this;
  }

  private static Map<String, String> getEditableMapOfVariables() {
    Class<?> classOfMap = System.getenv().getClass();
    try {
      return getFieldValue(classOfMap, System.getenv(), "m");
    } catch (Exception e) {
      LOG.error("Can't access the field 'm' of the map System.getenv() : ", e);
    }
    return null;
  }

  private static Map<String, String> getTheUnmodifiableEnvironment() {
    try {
      Class<?> processEnvironment = Class.forName("java.lang.ProcessEnvironment");
      Map<String, String> unmodifiableMap =
          getFieldValue(processEnvironment, null, "theUnmodifiableEnvironment");

      Class<?> collectionUnmodifiableMap = Class.forName("java.util.Collections$UnmodifiableMap");
      return getFieldValue(collectionUnmodifiableMap, unmodifiableMap, "m");
    } catch (Exception e) {
      LOG.error(
          "Can't access the field 'theUnmodifiableEnvironment' of java.lang.ProcessEnvironment : ",
          e);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, String> getFieldValue(Class<?> klass, Object object, String name)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = klass.getDeclaredField(name);
    field.setAccessible(true);
    return (Map<String, String>) field.get(object);
  }

  private void set(Map<String, String> variables, String name, String value) {
    if (variables != null) {
      if (value == null) {
        variables.remove(name);
      } else {
        variables.put(name, value);
        modified.add(name);
      }
    }
  }

  private void writeVariableToEnvMap(String name, String value) {
    set(getEditableMapOfVariables(), name, value);
    set(getTheUnmodifiableEnvironment(), name, value);
  }
}
