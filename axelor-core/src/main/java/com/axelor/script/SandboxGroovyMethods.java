/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import groovy.lang.Closure;
import groovy.lang.GString;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.ProcessGroovyMethods;
import org.codehaus.groovy.runtime.StringGroovyMethods;

/**
 * Groovy extension methods overriding the GDK methods that escape the script sandbox.
 *
 * <p>Registered as an extension module (see {@code
 * META-INF/groovy/org.codehaus.groovy.runtime.ExtensionModule}), these take precedence over the GDK
 * methods of the same signature, so they intercept the call however it is dispatched: direct call,
 * dynamic method name, {@code invokeMethod}, closure delegate, spread, etc. The receiver check
 * cannot catch these, as they run on allow-listed receivers ({@code String}, {@code List}, any
 * object).
 *
 * <ul>
 *   <li>{@code execute}: starts an operating system process
 *   <li>{@code use}: applies a category, exposing static methods of any class (e.g. {@code
 *       System.getenv}) as methods of allow-listed receivers
 *   <li>{@code mixin}: mixes any class (e.g. {@code GroovyShell}) into an allow-listed class, for
 *       the whole application
 *   <li>{@code setMetaClass}, {@code metaClass}: replace the metaclass of a class, for the whole
 *       application (e.g. removing the guard of {@link SandboxGuardedMethods})
 *   <li>{@code asType} ({@code as}): converting a list or map calls a constructor of the target
 *       class, so the target class must be allowed
 *   <li>{@code addShutdownHook}: registers code run when the JVM shuts down, outside of any
 *       evaluation (no timeout), for the whole application
 *   <li>{@code getAt} with a property name ({@code list['name']}): reads the property of each
 *       element of a collection or an array, which must be allowed as receivers
 * </ul>
 *
 * <p>They are denied only when called from sandboxed code; otherwise they delegate to the GDK.
 */
public final class SandboxGroovyMethods {

  private SandboxGroovyMethods() {}

  // ProcessGroovyMethods.execute

  public static Process execute(String self) throws IOException {
    GroovySandbox.denyFromSandbox("execute");
    return ProcessGroovyMethods.execute(self);
  }

  public static Process execute(String self, String[] envp, File dir) throws IOException {
    GroovySandbox.denyFromSandbox("execute");
    return ProcessGroovyMethods.execute(self, envp, dir);
  }

  public static Process execute(String self, List<?> envp, File dir) throws IOException {
    GroovySandbox.denyFromSandbox("execute");
    return ProcessGroovyMethods.execute(self, envp, dir);
  }

  public static Process execute(String[] commandArray) throws IOException {
    GroovySandbox.denyFromSandbox("execute");
    return ProcessGroovyMethods.execute(commandArray);
  }

  public static Process execute(String[] commandArray, String[] envp, File dir) throws IOException {
    GroovySandbox.denyFromSandbox("execute");
    return ProcessGroovyMethods.execute(commandArray, envp, dir);
  }

  public static Process execute(String[] commandArray, List<?> envp, File dir) throws IOException {
    GroovySandbox.denyFromSandbox("execute");
    return ProcessGroovyMethods.execute(commandArray, envp, dir);
  }

  public static Process execute(List<?> commands) throws IOException {
    GroovySandbox.denyFromSandbox("execute");
    return ProcessGroovyMethods.execute(commands);
  }

  public static Process execute(List<?> commands, String[] envp, File dir) throws IOException {
    GroovySandbox.denyFromSandbox("execute");
    return ProcessGroovyMethods.execute(commands, envp, dir);
  }

  public static Process execute(List<?> commands, List<?> envp, File dir) throws IOException {
    GroovySandbox.denyFromSandbox("execute");
    return ProcessGroovyMethods.execute(commands, envp, dir);
  }

  // DefaultGroovyMethods.use

  public static <T> T use(Object self, Class<?> categoryClass, Closure<T> closure) {
    GroovySandbox.denyFromSandbox("use");
    return DefaultGroovyMethods.use(self, categoryClass, closure);
  }

  public static <T> T use(Object self, List<Class> categoryClassList, Closure<T> closure) {
    GroovySandbox.denyFromSandbox("use");
    return DefaultGroovyMethods.use(self, categoryClassList, closure);
  }

  public static Object use(Object self, Object[] array) {
    GroovySandbox.denyFromSandbox("use");
    return DefaultGroovyMethods.use(self, array);
  }

  // DefaultGroovyMethods.setMetaClass, metaClass

  public static void setMetaClass(Class<?> self, MetaClass metaClass) {
    GroovySandbox.denyFromSandbox("setMetaClass");
    DefaultGroovyMethods.setMetaClass(self, metaClass);
  }

  public static void setMetaClass(Object self, MetaClass metaClass) {
    GroovySandbox.denyFromSandbox("setMetaClass");
    DefaultGroovyMethods.setMetaClass(self, metaClass);
  }

  public static void setMetaClass(GroovyObject self, MetaClass metaClass) {
    GroovySandbox.denyFromSandbox("setMetaClass");
    DefaultGroovyMethods.setMetaClass(self, metaClass);
  }

  public static MetaClass metaClass(Class<?> self, Closure<?> closure) {
    GroovySandbox.denyFromSandbox("metaClass");
    return DefaultGroovyMethods.metaClass(self, closure);
  }

  public static MetaClass metaClass(Object self, Closure<?> closure) {
    GroovySandbox.denyFromSandbox("metaClass");
    return DefaultGroovyMethods.metaClass(self, closure);
  }

  // DefaultGroovyMethods.asType, StringGroovyMethods.asType

  public static <T> T asType(Iterable<?> self, Class<T> type) {
    GroovySandbox.checkConversion(type);
    return DefaultGroovyMethods.asType(self, type);
  }

  public static <T> T asType(Collection<?> self, Class<T> type) {
    GroovySandbox.checkConversion(type);
    return DefaultGroovyMethods.asType(self, type);
  }

  public static <T> T asType(Object[] self, Class<T> type) {
    GroovySandbox.checkConversion(type);
    return DefaultGroovyMethods.asType(self, type);
  }

  public static <T> T asType(Closure<?> self, Class<T> type) {
    GroovySandbox.checkConversion(type);
    return DefaultGroovyMethods.asType(self, type);
  }

  public static <T> T asType(Map<?, ?> self, Class<T> type) {
    GroovySandbox.checkConversion(type);
    return DefaultGroovyMethods.asType(self, type);
  }

  public static <T> T asType(Number self, Class<T> type) {
    GroovySandbox.checkConversion(type);
    return DefaultGroovyMethods.asType(self, type);
  }

  public static <T> T asType(Object self, Class<T> type) {
    GroovySandbox.checkConversion(type);
    return DefaultGroovyMethods.asType(self, type);
  }

  public static <T> T asType(CharSequence self, Class<T> type) {
    GroovySandbox.checkConversion(type);
    return StringGroovyMethods.asType(self, type);
  }

  public static <T> T asType(GString self, Class<T> type) {
    GroovySandbox.checkConversion(type);
    return StringGroovyMethods.asType(self, type);
  }

  public static <T> T asType(String self, Class<T> type) {
    GroovySandbox.checkConversion(type);
    return StringGroovyMethods.asType(self, type);
  }

  // DefaultGroovyMethods.getAt(..., String): the receiver itself is checked by the transformations

  public static List getAt(Collection<?> self, String property) {
    GroovySandbox.checkElementsFromSandbox(self);
    return DefaultGroovyMethods.getAt(self, property);
  }

  public static Object getAt(Object self, String property) {
    if (self instanceof Object[] array) {
      GroovySandbox.checkElementsFromSandbox(Arrays.asList(array));
    }
    return DefaultGroovyMethods.getAt(self, property);
  }

  // DefaultGroovyMethods.addShutdownHook

  public static void addShutdownHook(Object self, Closure<?> closure) {
    GroovySandbox.denyFromSandbox("addShutdownHook");
    DefaultGroovyMethods.addShutdownHook(self, closure);
  }

  // DefaultGroovyMethods.mixin

  public static void mixin(MetaClass self, List<Class> categoryClasses) {
    GroovySandbox.denyFromSandbox("mixin");
    DefaultGroovyMethods.mixin(self, categoryClasses);
  }

  public static void mixin(Class<?> self, List<Class> categoryClasses) {
    GroovySandbox.denyFromSandbox("mixin");
    DefaultGroovyMethods.mixin(self, categoryClasses);
  }

  public static void mixin(Class<?> self, Class<?> categoryClass) {
    GroovySandbox.denyFromSandbox("mixin");
    DefaultGroovyMethods.mixin(self, categoryClass);
  }

  public static void mixin(Class<?> self, Class<?>[] categoryClass) {
    GroovySandbox.denyFromSandbox("mixin");
    DefaultGroovyMethods.mixin(self, categoryClass);
  }

  public static void mixin(MetaClass self, Class<?> categoryClass) {
    GroovySandbox.denyFromSandbox("mixin");
    DefaultGroovyMethods.mixin(self, categoryClass);
  }

  public static void mixin(MetaClass self, Class<?>[] categoryClass) {
    GroovySandbox.denyFromSandbox("mixin");
    DefaultGroovyMethods.mixin(self, categoryClass);
  }
}
