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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.rpc.Context;
import com.axelor.test.db.Contact;
import com.axelor.test.db.repo.ContactRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestEL extends ScriptTest {

  private static final String EXPR_INTERPOLATION =
      "fmt:text('(%s) = %s %s (%s) = (%s)', title.name, firstName, lastName, fullName, __user__)";

  private static final String EXPR_CONCAT =
      "'(' += title.name += ') = ' += firstName += ' ' += lastName += ' (' += fullName += ') = (' += str(__user__) += ')'";

  // false all, to evaluate all conditions
  private static final String EXPR_CONDITION =
      "(is(title, Contact) || fullName == 'foo') || is(__ref__, Title) || (__parent__ == null) || (__self__ == __this__)";

  // TODO: test should pass
  @Test
  @Disabled
  public void testImport() {
    Context context = context();
    ELScriptHelper helper = new ELScriptHelper(context);
    Object actual;

    // Not need of FQN
    actual = helper.eval("LocalDate.of(2020, 5, 22)");
    assertEquals(LocalDate.of(2020, 5, 22), actual);

    // __repo__
    actual = helper.eval("__repo__(Contact)");
    assertTrue(actual instanceof ContactRepository);

    // com.axelor.apps.tool imports
    actual = helper.eval("StringUtils.isBlank(\"\")");
    assertTrue((Boolean) actual);
  }

  @Test
  public void testEvalCast() {
    ScriptHelper helper = new ELScriptHelper(context());
    Object actual;

    actual = helper.eval("__parent__");
    assertTrue(actual instanceof Context);

    actual = helper.eval("__ref__");
    assertTrue(actual instanceof Contact);

    actual = helper.eval("__parent__.asType(Contact)");
    assertTrue(actual instanceof Contact);

    actual = helper.eval("__ref__.fullName");
    assertTrue(actual instanceof String);

    actual = helper.eval("__ref__.fullName += ' (" + 0 + ")'");
    assertEquals("Mr. John Smith (0)", actual);

    actual = helper.eval("__config__.string");
    assertEquals("some static text value", actual);

    actual = helper.eval("__config__.world");
    assertEquals("Hello world...", actual);

    actual = helper.eval("__config__.hello.contact()");
    assertTrue(actual instanceof Contact);
  }

  @Test
  public void testInterpolation() {
    ScriptHelper helper = new ELScriptHelper(context());
    Object result = helper.eval(EXPR_INTERPOLATION);

    assertEquals("(Mrs.) = John NAME (Mrs. John NAME) = (null)", result);
  }

  @Test
  public void testConcat() {
    ScriptHelper helper = new ELScriptHelper(context());
    Object result = helper.eval(EXPR_CONCAT);

    assertEquals("(Mrs.) = John NAME (Mrs. John NAME) = ()", result);
  }

  @Test
  public void testCondition() {
    ScriptHelper helper = new ELScriptHelper(context());
    Object result = helper.eval(EXPR_CONDITION);

    assertTrue((Boolean) result);
  }
}
