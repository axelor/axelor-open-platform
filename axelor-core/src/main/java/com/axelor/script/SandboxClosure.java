/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import groovy.lang.Binding;
import groovy.lang.Closure;
import java.util.Collection;
import java.util.Set;

/**
 * Base class of the closures compiled by {@link GroovyScriptSupport}.
 *
 * <p>Inside a closure, a call or property access without an explicit receiver is resolved on the
 * closure's owner and delegate, which the receiver checks never see. A closure whose delegate (or
 * owner) is a class the policy denies would therefore give access to it, for example its static
 * methods and properties ({@code c.delegate = System} then {@code getenv()}).
 *
 * <p>This class ensures that a sandboxed closure never holds such a delegate or owner: they can
 * only be changed through {@link #setDelegate(Object)} (used by {@code with}, {@code c.delegate =
 * ...}, {@code c['delegate'] = ...}, etc.) and {@link #rehydrate(Object, Object, Object)}, which
 * validate them against the policy. A collection or an array is validated with its elements, also
 * when a property is resolved on it, as its elements can be changed later.
 *
 * <p>The closures Groovy builds from a sandboxed closure ({@code curry}, {@code >>}, {@code
 * memoize}, {@code trampoline}...) are sandboxed too, see {@link SandboxClosureWrapper}.
 *
 * <p>The compiled closures are made to extend this class by {@link SandboxClosureProcessor}, as the
 * Groovy compiler always generates closures extending {@link Closure}.
 */
public abstract class SandboxClosure<V> extends Closure<V> implements SandboxCode {

  private static final long serialVersionUID = 1L;

  protected SandboxClosure(Object owner, Object thisObject) {
    super(owner, thisObject);
  }

  protected SandboxClosure(Object owner) {
    super(owner);
  }

  @Override
  public void setDelegate(Object delegate) {
    checkResolveTarget(delegate);
    super.setDelegate(delegate);
  }

  @Override
  public Closure<V> rehydrate(Object delegate, Object owner, Object thisObject) {
    checkResolveTarget(delegate);
    checkResolveTarget(owner);
    checkResolveTarget(thisObject);
    return super.rehydrate(delegate, owner, thisObject);
  }

  // The closures Groovy builds from this one are sandboxed too (see SandboxClosureWrapper).

  @Override
  public Closure<V> curry(Object... arguments) {
    return SandboxClosureWrapper.wrap(super.curry(arguments));
  }

  @Override
  public Closure<V> curry(Object argument) {
    return SandboxClosureWrapper.wrap(super.curry(argument));
  }

  @Override
  public Closure<V> rcurry(Object... arguments) {
    return SandboxClosureWrapper.wrap(super.rcurry(arguments));
  }

  @Override
  public Closure<V> rcurry(Object argument) {
    return SandboxClosureWrapper.wrap(super.rcurry(argument));
  }

  @Override
  public Closure<V> ncurry(int n, Object... arguments) {
    return SandboxClosureWrapper.wrap(super.ncurry(n, arguments));
  }

  @Override
  public Closure<V> ncurry(int n, Object argument) {
    return SandboxClosureWrapper.wrap(super.ncurry(n, argument));
  }

  @Override
  public <W> Closure<W> rightShift(Closure<W> other) {
    return SandboxClosureWrapper.wrap(super.rightShift(other));
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Closure<V> leftShift(Closure other) {
    return SandboxClosureWrapper.wrap(super.leftShift(other));
  }

  @Override
  public <W> Closure<W> andThen(Closure<W> other) {
    return SandboxClosureWrapper.wrap(super.andThen(other));
  }

  @Override
  public Closure<V> andThenSelf() {
    return SandboxClosureWrapper.wrap(super.andThenSelf());
  }

  @Override
  public Closure<V> andThenSelf(int times) {
    return SandboxClosureWrapper.wrap(super.andThenSelf(times));
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Closure<V> compose(Closure other) {
    return SandboxClosureWrapper.wrap(super.compose(other));
  }

  @Override
  public Closure<V> composeSelf() {
    return SandboxClosureWrapper.wrap(super.composeSelf());
  }

  @Override
  public Closure<V> composeSelf(int times) {
    return SandboxClosureWrapper.wrap(super.composeSelf(times));
  }

  @Override
  public Closure<V> memoize() {
    return SandboxClosureWrapper.wrap(super.memoize());
  }

  @Override
  public Closure<V> memoizeAtMost(int maxCacheSize) {
    return SandboxClosureWrapper.wrap(super.memoizeAtMost(maxCacheSize));
  }

  @Override
  public Closure<V> memoizeAtLeast(int protectedCacheSize) {
    return SandboxClosureWrapper.wrap(super.memoizeAtLeast(protectedCacheSize));
  }

  @Override
  public Closure<V> memoizeBetween(int protectedCacheSize, int maxCacheSize) {
    return SandboxClosureWrapper.wrap(super.memoizeBetween(protectedCacheSize, maxCacheSize));
  }

  @Override
  public Closure<V> trampoline(Object... arguments) {
    return SandboxClosureWrapper.wrap(super.trampoline(arguments));
  }

  @Override
  public Closure<V> trampoline() {
    return SandboxClosureWrapper.wrap(super.trampoline());
  }

  // A collection or an array delegate (or owner) reads a property from each of its elements, which
  // can be added after it was set (l = []; c.delegate = l; l << System): they are validated when a
  // property is resolved on it, and the property is read from the validated elements, see
  // GroovySandbox.checkProperty.

  /** The properties of the closure itself, not resolved on its owner or delegate. */
  private static final Set<String> CLOSURE_PROPERTIES =
      Set.of(
          "delegate",
          "owner",
          "maximumNumberOfParameters",
          "parameterTypes",
          "metaClass",
          "class",
          "directive",
          "resolveStrategy",
          "thisObject");

  @Override
  public Object getProperty(String property) {
    if (CLOSURE_PROPERTIES.contains(property)) {
      return super.getProperty(property);
    }
    final int strategy = getResolveStrategy();
    final Object delegate = getDelegate();
    final Object owner = getOwner();
    final Object delegateOwner =
        strategy != OWNER_ONLY && strategy != TO_SELF
            ? propertyOwner(delegate, property)
            : delegate;
    final Object ownerOwner =
        strategy != DELEGATE_ONLY && strategy != TO_SELF ? propertyOwner(owner, property) : owner;
    if (delegateOwner == delegate && ownerOwner == owner) {
      return super.getProperty(property);
    }
    // resolved on a copy: this closure is not changed while the property is read (the copy of a
    // SandboxClosureWrapper validates the delegate once more, before it is replaced here)
    final Closure<V> copy = super.rehydrate(delegateOwner, ownerOwner, getThisObject());
    return copy instanceof SandboxClosure<V> sandboxCopy
        ? sandboxCopy.superGetProperty(property)
        : copy.getProperty(property);
  }

  private Object superGetProperty(String property) {
    return super.getProperty(property);
  }

  private static Object propertyOwner(Object target, String property) {
    return target instanceof Collection<?> || target instanceof Object[]
        ? GroovySandbox.checkProperty(target, property)
        : target;
  }

  private static void checkResolveTarget(Object target) {
    // the variables of a template are provided by a Binding delegate
    if (target instanceof Binding) {
      return;
    }
    // a collection or an array is validated with its elements, whose properties are resolved
    GroovySandbox.checkProperty(target, null);
  }
}
