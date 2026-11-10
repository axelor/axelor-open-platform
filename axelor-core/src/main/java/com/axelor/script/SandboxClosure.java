/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import groovy.lang.Binding;
import groovy.lang.Closure;

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
 * validate them against the policy.
 *
 * <p>The compiled closures are made to extend this class by {@link SandboxClosureProcessor}, as the
 * Groovy compiler always generates closures extending {@link Closure}.
 */
public abstract class SandboxClosure<V> extends Closure<V> {

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

  private static void checkResolveTarget(Object target) {
    // the variables of a template are provided by a Binding delegate
    if (target instanceof Binding) {
      return;
    }
    GroovySandbox.check(target);
  }
}
