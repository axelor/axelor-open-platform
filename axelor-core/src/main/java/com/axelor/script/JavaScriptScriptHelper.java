/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.script;

import com.axelor.rpc.Context;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptException;
import org.graalvm.polyglot.Value;

public class JavaScriptScriptHelper extends AbstractScriptHelper {

  private org.graalvm.polyglot.Context context;

  public JavaScriptScriptHelper(Bindings bindings) {
    this.setBindings(bindings);
    this.context = createContext(bindings);
  }

  public JavaScriptScriptHelper(Context context) {
    this(new ScriptBindings(context));
  }

  private org.graalvm.polyglot.Context createContext(Bindings bindings) {
    org.graalvm.polyglot.Context ctx =
        org.graalvm.polyglot.Context.newBuilder()
            .allowExperimentalOptions(true)
            .allowAllAccess(true)
            .option("js.nashorn-compat", "true")
            .option("js.ecmascript-version", "latest")
            .build();

    ctx.getBindings("js").putMember("__scope", new JavaScriptScope(bindings));
    ctx.eval(
        "js",
        "Object.setPrototypeOf(globalThis, new Proxy(Object.prototype, {"
            + "  has(target, key) {"
            + "    return key in __scope || key in target;"
            + "  },"
            + "  get(target, key, receiver) {"
            + "    return Reflect.get((key in __scope) ? __scope : target, key, receiver);"
            + "  }"
            + "}))");

    return ctx;
  }

  @Override
  public Object eval(String expr, Bindings bindings) throws ScriptException {
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
