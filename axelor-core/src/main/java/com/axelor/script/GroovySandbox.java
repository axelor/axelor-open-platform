/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import com.axelor.event.Observes;
import com.axelor.events.ShutdownEvent;
import groovy.lang.Closure;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.codehaus.groovy.runtime.ComposedClosure;
import org.codehaus.groovy.runtime.CurriedClosure;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.MethodClosure;
import org.codehaus.groovy.runtime.memoize.Memoize;

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
  static final String CALL_CONSTRUCTED = "constructed";
  static final String CALL_CHECK_PROPERTY = "checkProperty";
  static final String CALL_METHOD_POINTER = "methodPointer";

  private static final ThreadLocal<Evaluation> CURRENT = new ThreadLocal<>();

  /** The closure returned by {@code trampoline()}, a class private to its package. */
  private static final String TRAMPOLINE_CLOSURE = "groovy.lang.TrampolineClosure";

  /** The closures returned by {@code memoize()} are private classes nested in {@code Memoize}. */
  private static final String MEMOIZE_FUNCTION_PREFIX = Memoize.class.getName() + "$";

  private static final StackWalker STACK_WALKER =
      StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

  /** Only instantiated by Guice, to observe the application shutdown. */
  GroovySandbox() {}

  /** Stops the coarse clock thread (see {@link Clock}), when the application stops. */
  void onAppShutdown(@Observes ShutdownEvent event) {
    if (clockStarted) {
      Clock.stop();
    }
  }

  /** An evaluation in progress on the current thread, until its deadline ({@code nanoTime}). */
  private record Evaluation(long deadline) {}

  /** Caps a timeout so that the deadline does not overflow. */
  private static final long MAX_TIMEOUT_NANOS = Long.MAX_VALUE / 4;

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
   * Starts an evaluation on the current thread, with the given timeout (in milliseconds; 0 or less
   * for none), until the returned scope is closed. Evaluations can be nested (a script calling a
   * service that evaluates another script): a nested evaluation can only tighten the enclosing
   * deadline, never extend it, and closing the scope restores the enclosing one.
   */
  public static Scope enter(long timeout) {
    final Evaluation previous = CURRENT.get();
    if (timeout > 0) {
      long deadline =
          System.nanoTime() + Math.min(TimeUnit.MILLISECONDS.toNanos(timeout), MAX_TIMEOUT_NANOS);
      if (previous != null && previous.deadline() - deadline < 0) {
        deadline = previous.deadline();
      }
      CURRENT.set(new Evaluation(deadline));
    }
    // no timeout: the enclosing deadline, if any, still applies
    return () -> {
      if (previous == null) {
        CURRENT.remove();
      } else {
        CURRENT.set(previous);
      }
    };
  }

  /**
   * Checks the execution timeout of the current evaluation, if any. Called by the compiled code on
   * each receiver check, and at the start of each loop iteration (a loop body may contain no call),
   * method and closure body (recursion, closures called by Java: each, times...). A call that
   * cannot be interrupted (a long database query) fails the evaluation at the next check after it
   * returns.
   */
  public static void timeout() {
    final Evaluation evaluation = CURRENT.get();
    if (evaluation != null && Clock.now() - evaluation.deadline() > 0) {
      throw new ScriptTimeoutException();
    }
  }

  /** Whether the {@link Clock} thread was started (on the first timeout check). */
  private static volatile boolean clockStarted;

  /**
   * A coarse clock: a thread stores {@link System#nanoTime()} every 10 ms, which is cheaper to read
   * on every timeout check (a field read) than the system clock. It is started on the first check,
   * and stopped when the application stops; the system clock is read from then on.
   */
  private static final class Clock {

    private static volatile long now = System.nanoTime();
    private static volatile boolean running = true;

    private static final ScheduledExecutorService TICKER =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "script-timeout-clock");
              t.setDaemon(true);
              return t;
            });

    static {
      TICKER.scheduleAtFixedRate(() -> now = System.nanoTime(), 10, 10, TimeUnit.MILLISECONDS);
      clockStarted = true;
    }

    static long now() {
      return running ? now : System.nanoTime();
    }

    static void stop() {
      running = false;
      TICKER.shutdownNow();
    }
  }

  /**
   * Validates an object used as a call receiver or property owner against the policy.
   *
   * <p>Classes compiled by the sandbox ({@link SandboxCode}: scripts, their closures and the
   * classes they declare) are allowed: their code is checked, and their supertypes are allowed.
   */
  public static <T> T check(T obj) {
    timeout();
    if (obj == null) {
      return obj;
    }

    Class<?> klass = obj.getClass();
    if (obj instanceof Class<?> type) klass = type;
    if (obj instanceof Executable executable) {
      checkReflected(executable);
      klass = executable.getDeclaringClass();
    }
    // an array of script objects (e.g. the values of a script enum) is checked as its elements
    while (klass.isArray()) {
      klass = klass.getComponentType();
    }

    if (!SandboxCode.class.isAssignableFrom(klass) && !isClosureWrapper(klass)) {
      policy().check(klass);
    }

    return obj;
  }

  /**
   * Whether the class is one of the closures Groovy builds from other closures ({@code curry},
   * {@code >>}, {@code memoize}, {@code trampoline}): calling them calls these closures, which the
   * script holds already.
   */
  private static boolean isClosureWrapper(Class<?> klass) {
    return klass == CurriedClosure.class
        || klass == ComposedClosure.class
        || (Closure.class.isAssignableFrom(klass)
            && (TRAMPOLINE_CLOSURE.equals(klass.getName())
                || klass.getName().startsWith(MEMOIZE_FUNCTION_PREFIX)));
  }

  /**
   * Validates the owner of a property access ({@code obj.name}), as {@link #check(Object)}.
   *
   * <p>A property that a collection or an array does not have itself is read from each of its
   * elements instead ({@code [a, b].name} is {@code [a.name, b.name]}), so the elements are
   * validated as owners too. The name is {@code null} when it is computed at runtime.
   */
  public static <T> T checkProperty(T obj, String name) {
    check(obj);
    if (obj instanceof Collection<?> collection) {
      if (name == null || !hasOwnProperty(obj, name)) {
        collection.forEach(GroovySandbox::check);
      }
    } else if (obj instanceof Object[] array) {
      if (name == null || !("length".equals(name) || hasOwnProperty(obj, name))) {
        Arrays.asList(array).forEach(GroovySandbox::check);
      }
    }
    return obj;
  }

  private static boolean hasOwnProperty(Object obj, String name) {
    return InvokerHelper.getMetaClass(obj).hasProperty(obj, name) != null;
  }

  /**
   * Validates the elements whose property a sandboxed subscript reads ({@code [a, b]['name']}), see
   * {@link SandboxGroovyMethods#getAt(Collection, String)}. Trusted code is not affected.
   */
  static void checkElementsFromSandbox(Iterable<?> elements) {
    Boolean sandboxed = null;
    for (Object element : elements) {
      if (element == null || isAllowedReceiver(element)) {
        continue;
      }
      if (sandboxed == null) {
        sandboxed = isSandboxed();
      }
      if (!sandboxed) {
        return;
      }
      check(element);
    }
  }

  /** Whether {@link #check(Object)} accepts the object without the reflection rules. */
  private static boolean isAllowedReceiver(Object obj) {
    if (obj instanceof Executable) {
      return false;
    }
    Class<?> klass = obj instanceof Class<?> type ? type : obj.getClass();
    while (klass.isArray()) {
      klass = klass.getComponentType();
    }
    return SandboxCode.class.isAssignableFrom(klass) || policy().allowed(klass);
  }

  /**
   * Returns a method pointer ({@code obj.&name}, {@code obj::name}, {@code Type::new}) whose owner
   * was validated by {@link #check(Object)}, wrapped so that its calls are validated too (see
   * {@link SandboxMethodPointer}).
   */
  public static Object methodPointer(Object pointer) {
    return pointer instanceof MethodClosure closure ? new SandboxMethodPointer(closure) : pointer;
  }

  /**
   * Returns the constructed object. Generated as {@code constructed(check(Type), new Type(...))}:
   * the first argument checks the class before the second one runs the constructor, so a denied
   * class is never constructed (its constructor could have side effects, e.g. create a file).
   */
  public static <T> T constructed(Object checkedType, T obj) {
    return obj;
  }

  /**
   * Denies a conversion ({@code as}, {@code asType}) from sandboxed code to a class the policy does
   * not allow: Groovy converts a list or map by calling a constructor of the target class.
   */
  static void checkConversion(Class<?> type) {
    if (type == null || !isConstructible(type) || policy().allowed(type)) {
      return;
    }
    if (isSandboxed()) {
      throw new ScriptPolicyException(
          String.format("Conversion to '%s' not allowed.", type.getName()));
    }
  }

  /**
   * Whether a conversion to the type may construct it: an interface or an abstract class cannot be
   * constructed (a map or closure is converted to a proxy). Sandbox classes are checked by the
   * policy themselves.
   */
  static boolean isConstructible(Class<?> type) {
    Class<?> klass = type;
    while (klass.isArray()) {
      klass = klass.getComponentType();
    }
    return !klass.isPrimitive()
        && !klass.isInterface()
        && !Modifier.isAbstract(klass.getModifiers())
        && !SandboxCode.class.isAssignableFrom(klass);
  }

  /**
   * A reflected method or constructor is checked as its declaring class. Only public members of
   * public classes can be used, so that reflection cannot reach what a direct call cannot (private
   * members, {@code setAccessible}), and guarded methods are rejected (see {@link
   * SandboxGuardedMethods}).
   */
  private static void checkReflected(Executable executable) {
    if (!Modifier.isPublic(executable.getModifiers())
        || !Modifier.isPublic(executable.getDeclaringClass().getModifiers())
        || (executable instanceof Method method && SandboxGuardedMethods.isGuarded(method))) {
      throw new ScriptPolicyException(
          String.format(
              "Reflective access to '%s.%s' not allowed.",
              executable.getDeclaringClass().getName(), executable.getName()));
    }
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
   * Denies an operation when it is called from sandboxed code, i.e. when code compiled by {@link
   * GroovyScriptSupport} ({@link SandboxCode}: a script or template, a closure or a class defined
   * in one) is on the call stack. Trusted code calling the same operation is not affected.
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
    return SandboxCode.class.isAssignableFrom(klass);
  }

  private static ScriptPolicy policy() {
    return ScriptPolicy.getInstance();
  }
}
