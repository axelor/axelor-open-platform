/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import groovy.lang.Closure;
import groovy.lang.MetaClass;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.ProcessGroovyMethods;

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
