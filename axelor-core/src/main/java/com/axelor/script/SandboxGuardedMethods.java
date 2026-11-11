/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import com.axelor.common.Inflections;
import groovy.lang.DelegatingMetaClass;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import groovy.lang.MetaClassRegistry;
import groovy.lang.MetaMethod;
import java.lang.reflect.Method;
import java.time.zone.ZoneRulesProvider;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import org.codehaus.groovy.reflection.CachedClass;

/**
 * Methods of allowed classes that scripts cannot call, as they read or change state shared by the
 * whole application (JVM defaults, system properties, shared rules).
 *
 * <p>The policy allows or denies whole classes, and these classes must stay allowed for their other
 * methods. Checking the method name at the call site would miss the other routes Groovy offers
 * (computed names, {@code invokeMethod}, closure delegates, method pointers, property syntax...),
 * which all go through the metaclass of the target class. So a guarding metaclass is installed on
 * the guarded classes: it rejects these methods when sandboxed code is on the call stack, and lets
 * trusted code call them. Reflective calls are rejected by {@link GroovySandbox#check(Object)}.
 */
final class SandboxGuardedMethods {

  /** The guarded methods, by declaring class. */
  private static final Map<Class<?>, Set<String>> METHODS =
      Map.of(
          // JVM defaults
          Locale.class, Set.of("setDefault"),
          TimeZone.class, Set.of("setDefault"),
          // time-zone rules of the JVM
          ZoneRulesProvider.class, Set.of("registerProvider", "refresh"),
          // instantiates any registered service provider
          ServiceLoader.class, Set.of("load", "loadInstalled"),
          // read system properties
          Integer.class, Set.of("getInteger"),
          Long.class, Set.of("getLong"),
          Boolean.class, Set.of("getBoolean"),
          // shared inflection rules
          Inflections.class, Set.of("ignore", "irregular", "singular", "plural"));

  /** Public JDK subclasses inheriting a guarded static method. */
  private static final Map<Class<?>, Class<?>> SUBCLASSES =
      Map.of(SimpleTimeZone.class, TimeZone.class);

  private SandboxGuardedMethods() {}

  /** The guarded classes: script classes cannot extend them, to not inherit their methods. */
  static Set<Class<?>> classes() {
    return METHODS.keySet();
  }

  /** Whether the reflected method is a guarded one. */
  static boolean isGuarded(Method method) {
    Set<String> names = METHODS.get(method.getDeclaringClass());
    return names != null && names.contains(method.getName());
  }

  /** Installs the guarding metaclasses. */
  static void install() {
    MetaClassRegistry registry = GroovySystem.getMetaClassRegistry();
    METHODS.forEach((type, names) -> guard(registry, type, names));
    SUBCLASSES.forEach((type, parent) -> guard(registry, type, METHODS.get(parent)));
  }

  private static void guard(MetaClassRegistry registry, Class<?> type, Set<String> names) {
    MetaClass current = registry.getMetaClass(type);
    if (!(current instanceof GuardingMetaClass)) {
      registry.setMetaClass(type, new GuardingMetaClass(current, names));
    }
  }

  private static void deny(String name) {
    GroovySandbox.denyFromSandbox(name);
  }

  /**
   * Wraps the metaclass of a guarded class. A {@link DelegatingMetaClass} (not a {@code
   * MetaClassImpl}) makes Groovy dispatch every call through these methods instead of selecting the
   * target method directly.
   */
  static final class GuardingMetaClass extends DelegatingMetaClass {

    private final Set<String> names;

    GuardingMetaClass(MetaClass delegate, Set<String> names) {
      super(delegate);
      this.names = names;
      initialize();
    }

    private void checkMethod(String name) {
      if (names.contains(name)) {
        deny(name);
      }
    }

    private void checkSetter(String property) {
      // replacing the metaclass would remove this guard
      if ("metaClass".equals(property)) {
        deny("metaClass");
      }
      if (property != null && !property.isEmpty()) {
        checkMethod("set" + Character.toUpperCase(property.charAt(0)) + property.substring(1));
      }
    }

    // Closure delegation (and other callers) pick the method and invoke it directly.

    @Override
    @SuppressWarnings("rawtypes")
    public MetaMethod pickMethod(String methodName, Class[] arguments) {
      return guarded(super.pickMethod(methodName, arguments));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public MetaMethod getMetaMethod(String name, Object[] args) {
      return guarded(super.getMetaMethod(name, args));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public MetaMethod getStaticMetaMethod(String name, Object[] args) {
      return guarded(super.getStaticMetaMethod(name, args));
    }

    private MetaMethod guarded(MetaMethod method) {
      return method != null && names.contains(method.getName())
          ? new GuardedMetaMethod(method)
          : method;
    }

    @Override
    public Object invokeMethod(Object object, String methodName, Object[] arguments) {
      checkMethod(methodName);
      return super.invokeMethod(object, methodName, arguments);
    }

    @Override
    public Object invokeMethod(Object object, String methodName, Object arguments) {
      checkMethod(methodName);
      return super.invokeMethod(object, methodName, arguments);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object invokeMethod(
        Class sender,
        Object receiver,
        String methodName,
        Object[] arguments,
        boolean isCallToSuper,
        boolean fromInsideClass) {
      checkMethod(methodName);
      return super.invokeMethod(
          sender, receiver, methodName, arguments, isCallToSuper, fromInsideClass);
    }

    @Override
    public Object invokeStaticMethod(Object object, String methodName, Object[] arguments) {
      checkMethod(methodName);
      return super.invokeStaticMethod(object, methodName, arguments);
    }

    @Override
    public void setProperty(Object object, String property, Object newValue) {
      checkSetter(property);
      super.setProperty(object, property, newValue);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void setProperty(
        Class sender,
        Object receiver,
        String property,
        Object value,
        boolean isCallToSuper,
        boolean fromInsideClass) {
      checkSetter(property);
      super.setProperty(sender, receiver, property, value, isCallToSuper, fromInsideClass);
    }
  }

  /** A guarded method, checked when invoked. */
  static final class GuardedMetaMethod extends MetaMethod {

    private final MetaMethod method;

    GuardedMetaMethod(MetaMethod method) {
      super(method.getNativeParameterTypes());
      this.method = method;
    }

    @Override
    public int getModifiers() {
      return method.getModifiers();
    }

    @Override
    public String getName() {
      return method.getName();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class getReturnType() {
      return method.getReturnType();
    }

    @Override
    public CachedClass getDeclaringClass() {
      return method.getDeclaringClass();
    }

    @Override
    public Object invoke(Object object, Object[] arguments) {
      deny(getName());
      return method.invoke(object, arguments);
    }
  }
}
