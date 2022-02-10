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
package com.axelor.text;

import com.axelor.script.ScriptTest;
import java.util.HashMap;
import java.util.Map;

public abstract class TemplateScriptTest extends ScriptTest {

  protected Map<String, Object> vars;

  @Override
  protected void prepareMoreData() {
    vars = new HashMap<>();
    vars.put("message", "Hello World!!!");

    vars.put("firstName", "John");
    vars.put("lastName", "Smith");

    vars.put("nested", new HashMap<>(vars));
  }
}
