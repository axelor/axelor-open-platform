/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import groovy.lang.Closure;

/**
 * A closure Groovy builds from a sandboxed closure ({@code curry}, {@code >>}, {@code memoize},
 * {@code trampoline}...), see {@link SandboxClosure}.
 *
 * <p>The closures Groovy builds are not sandboxed: their delegate and owner can be set to anything
 * ({@code c.memoize()} then {@code c.delegate = System}, or {@code rehydrate(System, ...)}), and
 * their properties and calls without a receiver are resolved on them. They are wrapped in this
 * class, whose delegate and owner are validated as those of any {@link SandboxClosure}.
 *
 * <p>The calls are delegated to the closure built by Groovy, kept in a {@link Target}, a class the
 * policy does not allow, as {@link SandboxMethodPointer} does.
 */
public final class SandboxClosureWrapper<V> extends SandboxClosure<V> {

  private static final long serialVersionUID = 1L;

  /** The closure returned by {@code trampoline()}, a class private to its package. */
  private static final String TRAMPOLINE_CLOSURE = "groovy.lang.TrampolineClosure";

  private final transient Target<V> target;

  private final boolean trampoline;

  private SandboxClosureWrapper(Closure<V> closure) {
    super(closure.getOwner(), closure.getThisObject());
    this.target = new Target<>(closure);
    this.trampoline = TRAMPOLINE_CLOSURE.equals(closure.getClass().getName());
    this.maximumNumberOfParameters = closure.getMaximumNumberOfParameters();
    this.parameterTypes = closure.getParameterTypes();
    super.setDelegate(closure.getDelegate());
    super.setResolveStrategy(closure.getResolveStrategy());
  }

  static <V> Closure<V> wrap(Closure<V> closure) {
    return closure instanceof SandboxCode ? closure : new SandboxClosureWrapper<>(closure);
  }

  // A copy (clone, with, tap, rehydrate...) wraps a copy of the closure built by Groovy: sharing it
  // would make the delegate set on the copy that of this closure too. A curried or composed closure
  // copies the closures it calls.
  @Override
  @SuppressWarnings("unchecked")
  public Object clone() {
    Closure<V> copy = wrap((Closure<V>) target.closure.clone());
    // the delegate set by rehydrate on this closure, not passed to the closure built by Groovy
    copy.setDelegate(getDelegate());
    copy.setResolveStrategy(getResolveStrategy());
    return copy;
  }

  @Override
  public void setDelegate(Object delegate) {
    super.setDelegate(delegate);
    // as the closure built by Groovy: a curried or composed closure sets it on its closures
    target.closure.setDelegate(delegate);
  }

  @Override
  public void setResolveStrategy(int resolveStrategy) {
    super.setResolveStrategy(resolveStrategy);
    target.closure.setResolveStrategy(resolveStrategy);
  }

  // As TrampolineClosure: the trampoline of a trampoline curries its closure, not the trampoline,
  // which would call a new trampoline loop on each bounce.

  @Override
  public Closure<V> trampoline(Object... arguments) {
    return trampoline ? wrap(target.closure.trampoline(arguments)) : super.trampoline(arguments);
  }

  @Override
  public Closure<V> trampoline() {
    return trampoline ? this : super.trampoline();
  }

  // Called as w(...) by a script, through the metaclass: Groovy passes a single null argument as a
  // null array.
  public V doCall(Object... arguments) {
    return call(arguments != null ? arguments : new Object[] {null});
  }

  // Called by call() and call(Object), as from collect: the arguments are passed unchanged, not
  // through the metaclass, which would unwrap a single array argument once more.
  @Override
  @SuppressWarnings("unchecked")
  public V call(Object... arguments) {
    Object result = target.closure.call(arguments);
    // a trampoline calls the closures it returns, until one returns something else: they are
    // wrapped as trampolines too (trampoline(...) of a sandboxed closure)
    while (trampoline && result instanceof SandboxClosureWrapper<?> next && next.trampoline) {
      result = next.target.closure.call();
    }
    return (V) result;
  }

  /** Holds the closure built by Groovy, out of reach of scripts (see the class documentation). */
  private static final class Target<V> {

    private final Closure<V> closure;

    private Target(Closure<V> closure) {
      this.closure = closure;
    }
  }
}
