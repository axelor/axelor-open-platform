/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
