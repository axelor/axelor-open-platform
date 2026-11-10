/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import groovy.lang.Binding;
import groovy.lang.Script;
import java.io.File;

/**
 * Base class of the scripts and templates compiled by {@link GroovyScriptSupport}.
 *
 * <p>{@link Script} methods that compile and run other code do so in a fresh {@code GroovyShell}
 * without the policy's compilation customizers, which would escape the sandbox. They are overridden
 * here to throw, so they are denied however they are dispatched (direct call, dynamic method name,
 * {@code invokeMethod}, closure delegation...).
 *
 * <p>This class also marks sandboxed code, see {@link GroovySandbox#denyFromSandbox(String)}.
 */
public abstract class SandboxScript extends Script implements SandboxCode {

  protected SandboxScript() {
    super();
  }

  protected SandboxScript(Binding binding) {
    super(binding);
  }

  @Override
  public Object evaluate(String expression) {
    throw denied("evaluate");
  }

  @Override
  public Object evaluate(File file) {
    throw denied("evaluate");
  }

  @Override
  public void run(File file, String[] arguments) {
    throw denied("run");
  }

  private static ScriptPolicyException denied(String name) {
    return new ScriptPolicyException(String.format("Method '%s' not allowed.", name));
  }
}
