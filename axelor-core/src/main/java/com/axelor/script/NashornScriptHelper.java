/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class NashornScriptHelper extends AbstractScriptHelper {

  private final ScriptEngine engine;

  public NashornScriptHelper(Bindings bindings) {
    this.setBindings(bindings);
    engine = new ScriptEngineManager().getEngineByName("nashorn");
    engine.setBindings(new NashornGlobals(engine), ScriptContext.GLOBAL_SCOPE);
  }

  public NashornScriptHelper(Context context) {
    this(new ScriptBindings(context));
  }

  @Override
  public Object eval(String expr, Bindings bindings) throws ScriptException {
    return engine.eval(expr, bindings);
  }
}
