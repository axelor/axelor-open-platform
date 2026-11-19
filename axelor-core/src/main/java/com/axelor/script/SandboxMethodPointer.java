/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import org.codehaus.groovy.runtime.MethodClosure;

/**
 * A method pointer created by a script ({@code obj.&name}, {@code obj::name}, {@code Type::new}).
 *
 * <p>Its owner is validated when it is created (see {@link GroovySandbox#methodPointer(Object)}),
 * but the receiver of its calls is not always the owner: when the owner is a class, Groovy calls an
 * instance method of that class on the first argument ({@code String::toUpperCase} applied to
 * {@code 'a'}). That argument is then validated as the receiver, as a direct call on it would be.
 *
 * <p>The calls are delegated to the {@link MethodClosure} created by Groovy, so they are resolved
 * exactly as Groovy does. It is kept in a {@link Target}, a class the policy does not allow: Groovy
 * reads private fields, and the method closure itself must not be called without this check (e.g.
 * by passing it to {@code each}). As a {@link SandboxClosure}, the pointer is sandboxed code, also
 * when trusted code calls it later.
 */
public final class SandboxMethodPointer extends SandboxClosure<Object> {

  private static final long serialVersionUID = 1L;

  private final transient Target target;

  SandboxMethodPointer(MethodClosure pointer) {
    super(pointer.getOwner());
    this.target = new Target(pointer);
    this.maximumNumberOfParameters = pointer.getMaximumNumberOfParameters();
    this.parameterTypes = pointer.getParameterTypes();
  }

  /** The name of the method, or {@code new} for a constructor. */
  public String getMethod() {
    return target.pointer.getMethod();
  }

  @Override
  public Object getProperty(String property) {
    // as MethodClosure, rather than resolving it on the owner
    return "method".equals(property) ? getMethod() : super.getProperty(property);
  }

  public Object doCall(Object... arguments) {
    if (target.pointer.getOwner() instanceof Class<?> type
        && arguments != null
        && arguments.length > 0
        && type.isInstance(arguments[0])) {
      GroovySandbox.check(arguments[0]);
    }
    return target.pointer.call(arguments);
  }

  /** Holds the method closure, out of reach of scripts (see the class documentation). */
  private static final class Target {

    private final MethodClosure pointer;

    private Target(MethodClosure pointer) {
      this.pointer = pointer;
    }
  }
}
