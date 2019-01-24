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
package com.axelor.script;

import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.JpaScanner;
import com.axelor.db.Model;
import com.axelor.db.Repository;
import com.axelor.inject.Beans;
import com.google.common.base.Preconditions;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Function;
import javax.management.Query;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

class NashornGlobals extends SimpleBindings {

  @FunctionalInterface
  public static interface VarArgs {
    Object apply(Object... args);
  }

  private static final Class<?>[] DEFAULT_TYPES = {
    Beans.class,
    Model.class,
    Query.class,
    Repository.class,
    LocalDate.class,
    LocalDateTime.class,
    LocalTime.class,
    ZonedDateTime.class,
    HashMap.class,
    HashSet.class,
    ArrayList.class,
    BigDecimal.class,
    MathContext.class,
    System.class,
    Arrays.class,
  };

  private JSObject typeFunction;

  private final ScriptEngine engine;

  public NashornGlobals(ScriptEngine engine) {
    this.engine = engine;

    this.put("__repo__", (Function<Class<? extends Model>, Object>) t -> JpaRepository.of(t));

    this.put(
        "doInJPA",
        (Function<ScriptObjectMirror, Object>)
            task -> {
              Preconditions.checkArgument(task.isFunction(), "doInJPA expectes function argument");
              final Object[] result = {null};
              JPA.runInTransaction(() -> result[0] = task.call(null, JPA.em()));
              return result[0];
            });

    this.put("listOf", (VarArgs) args -> new ArrayList<>(Arrays.asList(args)));
    this.put("setOf", (VarArgs) args -> new HashSet<>(Arrays.asList(args)));
    this.put(
        "mapOf",
        (Function<ScriptObjectMirror, Object>)
            obj -> {
              if (obj.isArray()) {
                throw new IllegalArgumentException("mapOf expectes object literal as argument");
              }
              return new HashMap<>(obj);
            });
  }

  private JSObject typeFunction() {
    if (typeFunction == null) {
      try {
        typeFunction = (JSObject) engine.eval("Java.type");
      } catch (ScriptException e) {
        // this should never happen
        throw new RuntimeException(e);
      }
    }
    return typeFunction;
  }

  private Class<?> findClass(String simpleName) {
    if (StringUtils.isBlank(simpleName) || !Character.isUpperCase(simpleName.charAt(0))) {
      return null;
    }

    Class<?> found =
        Arrays.stream(DEFAULT_TYPES)
            .filter(c -> c.getSimpleName().equals(simpleName))
            .findFirst()
            .orElse(null);

    if (found == null) {
      found = JpaScanner.findModel(simpleName);
    }
    if (found == null) {
      found = JpaScanner.findRepository(simpleName);
    }
    return found;
  }

  public Object get(Object key) {
    if (super.containsKey(key)) {
      return super.get(key);
    }
    final Object value = super.get(key);
    if (value == null && key instanceof String) {
      final Class<?> klass = findClass((String) key);
      return klass == null ? null : typeFunction().call(null, klass.getName());
    }
    return value;
  }

  public boolean containsKey(Object key) {
    boolean contains = super.containsKey(key);
    if (contains || !(key instanceof String)) {
      return contains;
    }
    return findClass((String) key) != null;
  };
}
