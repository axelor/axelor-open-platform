/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.JpaScanner;
import com.axelor.db.Model;
import com.axelor.db.Repository;
import com.axelor.inject.Beans;
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
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import javax.management.Query;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

class JavaScriptScope implements ProxyObject {

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

  private final Bindings globals;

  private final Bindings bindings;

  public JavaScriptScope(Bindings bindings) {
    this.globals = new SimpleBindings();
    this.bindings = bindings;

    globals.put("__repo__", (Function<Class<? extends Model>, Object>) t -> JpaRepository.of(t));
    globals.put("doInJPA", (Function<Function<Object[], Object>, ?>) this::doInJPA);
  }

  private Object doInJPA(Function<Object[], Object> task) {
    final AtomicReference<Object> result = new AtomicReference<>(null);
    JPA.runInTransaction(() -> result.set(task.apply(new Object[] {JPA.em()})));
    return result.get();
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

  @Override
  public Object getMember(String key) {
    return globals.computeIfAbsent(key, x -> bindings.computeIfAbsent(x, this::findClass));
  }

  @Override
  public Object getMemberKeys() {
    Set<String> names = new HashSet<>();
    names.addAll(globals.keySet());
    names.addAll(bindings.keySet());
    return names.toArray();
  }

  @Override
  public boolean hasMember(String key) {
    return globals.containsKey(key) || bindings.containsKey(key) || findClass(key) != null;
  }

  @Override
  public void putMember(String key, Value value) {}
}
