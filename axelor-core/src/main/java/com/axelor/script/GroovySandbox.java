/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import com.axelor.event.Observes;
import com.axelor.events.ShutdownEvent;
import groovy.lang.DelegatingMetaClass;
import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaBeanProperty;
import groovy.lang.MetaClass;
import groovy.lang.MetaMethod;
import groovy.lang.MetaProperty;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.codehaus.groovy.reflection.CachedField;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.MethodClosure;
import org.codehaus.groovy.runtime.metaclass.MultipleSetterProperty;

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
  static final String CALL_CHECK_SPREAD_PROPERTY = "checkSpreadProperty";
  static final String CALL_TIMEOUT = "timeout";
  static final String CALL_CONSTRUCTED = "constructed";
  static final String CALL_CHECK_PROPERTY = "checkProperty";
  static final String CALL_METHOD_POINTER = "methodPointer";

  private static final ThreadLocal<Evaluation> CURRENT = new ThreadLocal<>();

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

    if (!SandboxCode.class.isAssignableFrom(klass)) {
      policy().check(klass);
    }

    return obj;
  }

  /**
   * Validates the owner of a property access ({@code obj.name}), as {@link #check(Object)}.
   *
   * <p>A property that a collection or an array does not have itself is read from each of its
   * elements instead ({@code [a, b].name} is {@code [a.name, b.name]}), so the elements are
   * validated as owners too, at any depth ({@code [[a]].name} is {@code [[a.name]]}). They are
   * collected in a single pass, and the property is read from them by the {@link PropertyOwners}
   * returned in place of the container: a stateful collection could give other elements to a second
   * iteration. The name is {@code null} when it is computed at runtime.
   *
   * <p>A collection overriding {@code getProperty(String)} reads its properties through its
   * override, which is called as in Groovy: a collection declared in a script has its elements
   * checked by its metaclass (see {@link #readsThroughOverride(Object)}), and the elements of
   * another one are checked here first.
   */
  public static Object checkProperty(Object obj, String name) {
    check(obj);
    return propertyOwner(obj, name, GroovySandbox::check);
  }

  /**
   * The owner a property is read from: the object itself, or the {@link PropertyOwners} of a
   * collection or an array whose property is read from its elements, each passed to the check.
   */
  private static Object propertyOwner(Object obj, String name, Consumer<Object> check) {
    final Iterable<?> elements = ownerElements(obj, name);
    if (elements == null) {
      return obj;
    }
    final PropertyOwners owners = collectOwners(obj, elements, name, check, null);
    // its override of getProperty is called once its elements are checked
    return overridesGetProperty(obj) ? obj : owners;
  }

  /**
   * The elements of a collection or an array whose property is read from each of them: the property
   * is not one of the container itself (any property when the name is {@code null}). Returns {@code
   * null} for other objects.
   */
  private static Iterable<?> propertyElements(Object obj, String name) {
    if (obj instanceof Collection<?> collection) {
      // a property of the collection itself (looked up first: the most common, and found faster),
      // or a collection reading any property itself, is not read from its elements
      return (name != null && hasOwnProperty(obj, name)) || readsAnyProperty(obj)
          ? null
          : collection;
    }
    if (obj instanceof Object[] array) {
      return name == null || !("length".equals(name) || hasOwnProperty(obj, name))
          ? Arrays.asList(array)
          : null;
    }
    return null;
  }

  /**
   * The elements whose property is read in place of the object's, as {@link
   * #propertyElements(Object, String)}, except for an object reading its properties through its own
   * {@code getProperty(String)}, which is called instead.
   */
  private static Iterable<?> ownerElements(Object obj, String name) {
    return readsThroughOverride(obj) ? null : propertyElements(obj, name);
  }

  /**
   * Whether Groovy reads the properties of the object through an override of {@code
   * getProperty(String)} that its metaclass checks: a collection declared in a script, whose {@link
   * CollectionMetaClass} validates the elements the override reads from ({@code
   * super.getProperty(name)}). Its override is called as in Groovy, rather than the property being
   * read from its elements.
   */
  private static boolean readsThroughOverride(Object obj) {
    return overridesGetProperty(obj)
        && ((GroovyObject) obj).getMetaClass() instanceof CollectionMetaClass;
  }

  /** Whether the classes override the default {@link GroovyObject#getProperty(String)}. */
  private static final ClassValue<Boolean> GET_PROPERTY_OVERRIDES =
      new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
          try {
            return type.getMethod("getProperty", String.class).getDeclaringClass()
                != GroovyObject.class;
          } catch (NoSuchMethodException e) {
            return false;
          }
        }
      };

  /** Whether the object overrides the default {@link GroovyObject#getProperty(String)}. */
  private static boolean overridesGetProperty(Object obj) {
    return obj instanceof GroovyObject && GET_PROPERTY_OVERRIDES.get(obj.getClass());
  }

  /**
   * Collects each element, passed to the check, in a single pass: a nested container whose property
   * is read from its elements is collected as well, at any depth. A container is visited once, so
   * that a container holding itself does not loop forever.
   */
  private static PropertyOwners collectOwners(
      Object container,
      Iterable<?> elements,
      String name,
      Consumer<Object> check,
      Map<Object, PropertyOwners> visited) {
    final PropertyOwners owners = new PropertyOwners(container, name);
    if (visited != null) {
      visited.put(container, owners);
    }
    for (Object element : elements) {
      check.accept(element);
      final Iterable<?> nested = ownerElements(element, name);
      if (nested == null) {
        owners.elements.add(element);
        continue;
      }
      if (visited == null) {
        visited = new IdentityHashMap<>();
        visited.put(container, owners);
      }
      final PropertyOwners seen = visited.get(element);
      final PropertyOwners collected =
          seen != null ? seen : collectOwners(element, nested, name, check, visited);
      // an element overriding getProperty is read through its override, see checkProperty
      owners.elements.add(overridesGetProperty(element) ? element : collected);
    }
    return owners;
  }

  /**
   * The validated elements of a collection or an array whose property is read from each of them, in
   * place of the container as the owner of a property access, see {@link #checkProperty(Object,
   * String)}.
   *
   * <p>The property is read from these elements as Groovy reads it from a collection, not from the
   * container, which would be iterated again. A property the container has itself, known only at
   * this point for a name computed at runtime, is read from the container. A property is set on the
   * container, as Groovy does not set it on its elements.
   */
  static final class PropertyOwners implements GroovyObject {

    private final Object container;

    /** The name the elements were collected for, {@code null} when computed at runtime. */
    private final String name;

    private final List<Object> elements = new ArrayList<>();

    private PropertyOwners(Object container, String name) {
      this.container = container;
      this.name = name;
    }

    @Override
    public Object getProperty(String name) {
      // the elements were collected for this name as it is not a property of the container: not
      // looked up again, a missing property being the slowest lookup (a nested PropertyOwners reads
      // the property from its own elements)
      return name.equals(this.name) || propertyElements(container, name) != null
          ? DefaultGroovyMethods.getAt(elements, name)
          : InvokerHelper.getProperty(container, name);
    }

    @Override
    public void setProperty(String name, Object value) {
      InvokerHelper.setProperty(container, name, value);
    }

    @Override
    public MetaClass getMetaClass() {
      return InvokerHelper.getMetaClass(PropertyOwners.class);
    }

    @Override
    public void setMetaClass(MetaClass metaClass) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Whether Groovy reads the property from the object itself rather than from each of its elements:
   * through a getter, or a field it can read. A setter alone is not read, and a non-public field is
   * not readable when its package is not opened to Groovy (that of most JDK collections, unless the
   * JVM is started with {@code --add-opens}, as by Tomcat): Groovy then reads the property from
   * each element.
   */
  private static boolean hasOwnProperty(Object obj, String name) {
    MetaProperty property = InvokerHelper.getMetaClass(obj).hasProperty(obj, name);
    if (property instanceof MetaBeanProperty bean) {
      if (bean.getGetter() != null) {
        return true;
      }
      property = bean.getField();
    } else if (property instanceof MultipleSetterProperty multiple) {
      if (multiple.getGetter() != null) {
        return true;
      }
      property = multiple.getField();
    }
    return property instanceof CachedField field && isReadable(field, obj);
  }

  /**
   * Whether Groovy reads any property from the object itself, as {@code MetaClassImpl} does before
   * reading it from each element of a collection: a map from its entries, and other objects through
   * a generic {@code get(String)} method.
   *
   * <p>A {@link GroovyObject} overriding {@code getProperty(String)} is not taken as reading them
   * itself: its override can read them from each element through the metaclass ({@code
   * super.getProperty(name)}), see {@link #readsThroughOverride(Object)}.
   */
  private static boolean readsAnyProperty(Object obj) {
    if (obj instanceof Map) {
      return true;
    }
    final MetaMethod get =
        InvokerHelper.getMetaClass(obj).getMetaMethod("get", new Object[] {String.class});
    return get != null
        && get.getParameterTypes().length == 1
        && get.getParameterTypes()[0].getTheClass() == String.class;
  }

  /**
   * Whether Groovy can read the field of the object: {@link CachedField#getProperty(Object)} makes
   * it accessible first ({@link CachedField#getCachedField()} does the same), and reads the
   * property from each element instead when it cannot.
   */
  private static boolean isReadable(CachedField field, Object obj) {
    final Field reflected = field.getCachedField();
    return reflected.canAccess(Modifier.isStatic(reflected.getModifiers()) ? null : obj);
  }

  /**
   * The metaclass of the collections declared in scripts, returned by their {@code getMetaClass()}
   * (see {@link GroovyClassCheck}).
   */
  private static final ClassValue<MetaClass> COLLECTION_META_CLASSES =
      new ClassValue<>() {
        @Override
        protected MetaClass computeValue(Class<?> type) {
          return new CollectionMetaClass(GroovySystem.getMetaClassRegistry().getMetaClass(type));
        }
      };

  /**
   * The metaclass of a collection declared in a script, see {@link CollectionMetaClass}. Called by
   * the {@code getMetaClass()} method {@link GroovyClassCheck} adds to these classes.
   */
  public static MetaClass collectionMetaClass(Object collection) {
    return COLLECTION_META_CLASSES.get(collection.getClass());
  }

  /**
   * The metaclass of a collection declared in a script: it validates the elements a property is
   * read from, as {@link #checkProperty(Object, String)}, and reads it from the validated elements.
   *
   * <p>A property access in a script is checked where it is written, but Groovy also reads a
   * property through the metaclass from other places, unchecked: {@code getProperty(name)} (called
   * directly, by {@code invokeMethod} or a method pointer, or from an override as {@code
   * super.getProperty(name)}), and the properties a method of the class reads on {@code this}
   * ({@code def m() { name }}). For a collection, the metaclass reads a property it does not have
   * from each of its elements, so they are validated here. Trusted code is not affected.
   *
   * <p>The elements of the collection itself are always validated here, even when its class
   * overrides {@code getProperty(String)}: the override reads them through this metaclass.
   */
  static final class CollectionMetaClass extends DelegatingMetaClass {

    private CollectionMetaClass(MetaClass delegate) {
      super(delegate);
      initialize();
    }

    @Override
    public Object getProperty(Object object, String property) {
      return readProperty(object, property, () -> super.getProperty(object, property));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object getProperty(
        Class sender,
        Object receiver,
        String property,
        boolean isCallToSuper,
        boolean fromInsideClass) {
      return readProperty(
          receiver,
          property,
          () -> super.getProperty(sender, receiver, property, isCallToSuper, fromInsideClass));
    }

    private static Object readProperty(Object obj, String name, Supplier<Object> read) {
      final Iterable<?> elements = propertyElements(obj, name);
      if (elements == null) {
        return read.get();
      }
      return DefaultGroovyMethods.getAt(
          collectOwners(obj, elements, name, checkFromSandbox(), null).elements, name);
    }
  }

  /**
   * The owner of a property a sandboxed subscript reads ({@code obj['name']}), as {@link
   * #checkProperty(Object, String)}, see {@link SandboxGroovyMethods#getAt(Object, String)}.
   * Trusted code is not affected: the elements are not checked.
   *
   * <p>The elements of a collection or an array are checked even for a property of the container
   * itself, in case Groovy cannot read it and reads it from each element instead.
   */
  static Object propertyOwnerFromSandbox(Object obj, String name) {
    final Consumer<Object> check = checkFromSandbox();
    final Object owner = propertyOwner(obj, name, check);
    if (owner == obj) {
      final Iterable<?> elements = propertyElements(obj, null);
      if (elements != null) {
        collectOwners(obj, elements, name, check, null);
      }
    }
    return owner;
  }

  /**
   * The validated elements of a collection whose property a sandboxed subscript reads from each of
   * them ({@code [a, b]['name']}), as {@link #checkProperty(Object, String)}, see {@link
   * SandboxGroovyMethods#getAt(Collection, String)}. Trusted code is not affected: the elements are
   * not checked.
   */
  static List<Object> propertyOwnersFromSandbox(Collection<?> collection, String name) {
    return collectOwners(collection, collection, name, checkFromSandbox(), null).elements;
  }

  /** Checks the elements only when called from sandboxed code, found on the first one to check. */
  private static Consumer<Object> checkFromSandbox() {
    final Boolean[] sandboxed = {null};
    return element -> {
      if (element == null || isAllowedReceiver(element)) {
        return;
      }
      if (sandboxed[0] == null) {
        sandboxed[0] = isSandboxed();
      }
      if (sandboxed[0]) {
        check(element);
      }
    };
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
   * not allow: Groovy converts a list or map by calling a constructor of the target class, and a
   * map or a closure to an abstract class by constructing a proxy subclass of it.
   */
  static void checkConversion(Class<?> type, Object value) {
    if (type == null
        || type.isInstance(value)
        || !isConstructible(type)
        || policy().allowed(type)) {
      return;
    }
    if (isSandboxed()) {
      throw new ScriptPolicyException(
          String.format("Conversion to '%s' not allowed.", type.getName()));
    }
  }

  /**
   * Whether a conversion to the type may construct it or a proxy subclass of it: concrete classes
   * are constructed from lists or maps, and abstract classes are proxied from maps or closures.
   * Interfaces and primitives cannot be constructed this way; sandbox classes are checked by the
   * policy themselves.
   */
  static boolean isConstructible(Class<?> type) {
    Class<?> klass = type;
    while (klass.isArray()) {
      klass = klass.getComponentType();
    }
    return !klass.isPrimitive()
        && !klass.isInterface()
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
   * Validates the receiver of a spread property access ({@code recv*.name}), as {@link
   * #checkSpread(Object)}.
   *
   * <p>The property is read from each element, so each element is validated as a property owner
   * ({@link #checkProperty(Object, String)}): an element that is a collection or an array reads the
   * property from its own elements, at any depth ({@code [[a]]*.name} reads {@code a.name}).
   */
  public static Object checkSpreadProperty(Object obj, String name) {
    if (obj == null) {
      return null;
    }

    check(obj);

    final List<Object> elements = new ArrayList<>();
    for (Iterator<?> it = InvokerHelper.asIterator(obj); it.hasNext(); ) {
      elements.add(checkProperty(it.next(), name));
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
