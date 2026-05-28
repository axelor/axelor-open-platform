/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import com.axelor.rpc.Context;
import com.axelor.script.GroovyScriptSupport.PolicyChecker;
import groovy.lang.Binding;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;
import javax.script.Bindings;

public class GroovyScriptHelper extends AbstractScriptHelper {

  private long timeout;

  public GroovyScriptHelper(Bindings bindings) {
    this.setBindings(bindings);
  }

  public GroovyScriptHelper(Context context) {
    this(new ScriptBindings(context));
  }

  public GroovyScriptHelper withTimeout(long timeout) {
    this.timeout = timeout;
    return this;
  }

  @Override
  public Object eval(String expr, Bindings bindings) throws Exception {
    Script script = GroovyScriptSupport.createScript(expr);
    PolicyChecker checker = timeout > 0 ? new PolicyChecker(timeout) : new PolicyChecker();

    script.setBinding(
        new Binding(bindings) {

          @Override
          public Object getVariable(String name) {
            if (PolicyChecker.NAME.equals(name)) return checker;
            try {
              return super.getVariable(name);
            } catch (MissingPropertyException e) {
              if (name.startsWith("_") || name.startsWith("$")) {
                return null;
              }
              if (!"out".equals(name)) {
                log.warn("No such field in: {} -- ({})", expr, name);
              }
              return null;
            }
          }
        });
    return script.run();
  }
}
