/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * Runtime checks of the Groovy script sandbox.
 *
 * <p>The policy transformations ({@link GroovyCheckTransformation}, {@link
 * GroovyCheckPropTransformation}) compile calls to these static methods directly into the script.
 * They are static on purpose: a script cannot shadow or replace a class reference, whereas a
 * variable holding a checker could be shadowed by a local variable or resolved on a closure
 * delegate.
 *
 * <p>Per-evaluation state (the execution timeout) is attached to the current thread with {@link
 * #enter()} for the duration of an evaluation.
 *
 * <p>This class is internal to the scripting support. It is public only so that compiled scripts
 * can call it; scripts themselves cannot reference it as it is not allowed by the policy.
 */
public final class GroovySandbox {

  static final String CALL_CHECK = "check";
  static final String CALL_CHECK_SPREAD = "checkSpread";
  static final String CALL_TIMEOUT = "timeout";

  private static final ThreadLocal<Evaluation> CURRENT = new ThreadLocal<>();

  private static final StackWalker STACK_WALKER =
      StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

  private GroovySandbox() {}

  /** An evaluation in progress on the current thread. */
  private record Evaluation(long start, long timeout) {}

  /** Ends the evaluation started by {@link #enter()} when closed. */
  @FunctionalInterface
  public interface Scope extends AutoCloseable {
    @Override
    void close();
  }

  /**
   * Starts an evaluation on the current thread, with the application script timeout, until the
   * returned scope is closed.
   */
  public static Scope enter() {
    return enter(policy().getTimeout());
  }

  /**
   * Starts an evaluation on the current thread, with the given timeout (in milliseconds), until the
   * returned scope is closed. Evaluations can be nested (a script calling a service that evaluates
   * another script): closing the scope restores the enclosing one.
   */
  public static Scope enter(long timeout) {
    final Evaluation previous = CURRENT.get();
    CURRENT.set(new Evaluation(System.currentTimeMillis(), timeout));
    return () -> {
      if (previous == null) {
        CURRENT.remove();
      } else {
        CURRENT.set(previous);
      }
    };
  }

  /** Checks the execution timeout of the current evaluation, if any. */
  public static void timeout() {
    final Evaluation evaluation = CURRENT.get();
    if (evaluation != null
        && System.currentTimeMillis() - evaluation.start() > evaluation.timeout()) {
      throw new ScriptTimeoutException();
    }
  }

  /** Validates an object used as a call receiver or property owner against the policy. */
  public static <T> T check(T obj) {
    if (obj == null || obj instanceof SandboxScript) {
      return obj;
    }

    Class<?> klass = obj.getClass();
    if (obj instanceof Class<?> type) klass = type;
    if (obj instanceof Executable executable) klass = executable.getDeclaringClass();

    policy().check(klass);

    return obj;
  }

  /**
   * Validates the receiver of a spread call or property access ({@code recv*.name}).
   *
   * <p>The call is dispatched on each element of the receiver, not on the receiver itself, so each
   * element is validated as a receiver (as {@link #check(Object)}). The elements are collected the
   * same way Groovy iterates them for the spread (including iterators, maps, etc.), and that list
   * is returned to be spread instead, so the receiver is iterated only once.
   */
  public static Object checkSpread(Object obj) {
    if (obj == null) {
      return null;
    }

    check(obj);

    final List<Object> elements = new ArrayList<>();
    for (Iterator<?> it = InvokerHelper.asIterator(obj); it.hasNext(); ) {
      elements.add(check(it.next()));
    }

    return elements;
  }

  /**
   * Denies an operation when it is called from sandboxed code, i.e. when a script or template
   * compiled by {@link GroovyScriptSupport} (or a closure defined in one) is on the call stack.
   * Trusted code calling the same operation is not affected.
   */
  static void denyFromSandbox(String operation) {
    if (isSandboxed()) {
      throw new ScriptPolicyException(String.format("Method '%s' not allowed.", operation));
    }
  }

  private static boolean isSandboxed() {
    return STACK_WALKER.walk(
        frames -> frames.anyMatch(frame -> isSandboxClass(frame.getDeclaringClass())));
  }

  private static boolean isSandboxClass(Class<?> klass) {
    // closures are compiled as inner classes of the script class
    for (Class<?> type = klass; type != null; type = type.getEnclosingClass()) {
      if (SandboxScript.class.isAssignableFrom(type)) {
        return true;
      }
    }
    return false;
  }

  private static ScriptPolicy policy() {
    return ScriptPolicy.getInstance();
  }
}
