/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import com.axelor.rpc.Context;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.management.ExecutionListener;

public class JavaScriptScriptHelper extends AbstractScriptHelper {

  private org.graalvm.polyglot.Context context;

  private long timeout;

  private static final ScriptPolicy SCRIPT_POLICY = ScriptPolicy.getInstance();

  public JavaScriptScriptHelper(Bindings bindings) {
    this.setBindings(bindings);
    this.context = createContext(bindings);
  }

  public JavaScriptScriptHelper(Context context) {
    this(new ScriptBindings(context));
  }

  private boolean lookup(String className) {
    try {
      Class<?> klass = Class.forName(className);
      return SCRIPT_POLICY.allowed(klass);
    } catch (Exception e) {
      return false;
    }
  }

  private org.graalvm.polyglot.Context createContext(Bindings bindings) {
    org.graalvm.polyglot.Context ctx =
        org.graalvm.polyglot.Context.newBuilder()
            .allowExperimentalOptions(true)
            .allowAllAccess(true)
            .allowHostClassLookup(this::lookup)
            .option("engine.WarnInterpreterOnly", "false")
            .option("js.nashorn-compat", "true")
            .option("js.ecmascript-version", "latest")
            .build();

    ctx.getBindings("js").putMember("__scope", new JavaScriptScope(bindings, SCRIPT_POLICY));
    ctx.eval(
        "js",
        """
        Object.setPrototypeOf(globalThis, new Proxy(Object.prototype, {\
          has(target, key) {\
            return key in __scope || key in target;\
          },\
          get(target, key, receiver) {\
            return Reflect.get((key in __scope) ? __scope : target, key, receiver);\
          }\
        }))""");

    return ctx;
  }

  public JavaScriptScriptHelper withTimeout(long timeout) {
    this.timeout = timeout;
    return this;
  }

  @Override
  public Object eval(String expr, Bindings bindings) throws ScriptException {
    long start = System.currentTimeMillis();
    long timeout = this.timeout > 0 ? this.timeout : SCRIPT_POLICY.getTimeout();

    ExecutionListener listener =
        ExecutionListener.newBuilder()
            .onEnter(
                e -> {
                  long time = System.currentTimeMillis();
                  if (time - start > timeout) {
                    throw new ScriptTimeoutException();
                  }
                })
            .statements(true)
            .attach(context.getEngine());
    try {
      return doEval(expr, bindings);
    } finally {
      listener.close();
    }
  }

  public Object doEval(String expr, Bindings bindings) throws ScriptException {
    if (getBindings() != bindings) {
      throw new IllegalArgumentException(
          "Evaluating JavaScript with different bindings is not supported.");
    }

    final Value value = context.eval("js", expr);

    if (value.isException()) {
      throw value.throwException();
    }

    if (value.isNull()) return null;
    if (value.isHostObject()) return value.asHostObject();

    if (value.isBoolean()) return value.asBoolean();
    if (value.isString()) return value.asString();

    if (value.isNumber()) {
      if (value.toString().contains(".")) return value.asDouble();
      if (value.fitsInInt()) return value.asInt();
      if (value.fitsInLong()) return value.asLong();
    }

    if (value.isDate()) return value.asDate();
    if (value.isTime()) return value.asTime();
    if (value.isDuration()) return value.asDuration();
    if (value.isTimeZone()) return value.asTimeZone();
    if (value.isInstant()) return value.asInstant();

    // Convert array like value to List
    if (value.hasArrayElements()) return value.as(List.class);

    // Convert object like value to Map
    if (value.hasMembers()) return value.as(Map.class);

    throw new ScriptException("Invalid result from script: " + expr);
  }
}
