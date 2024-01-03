/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.rpc.Context;
import com.axelor.test.db.Contact;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestJavaScript extends ScriptTest {

  private static final int COUNT = 1000;

  private static final String EXPR_INTERPOLATION =
      "`(${title.name}) = ${firstName} ${lastName} (${fullName}) = (${__user__})`";

  private static final String EXPR_CONCAT =
      "'(' + title.name + ') = ' + firstName + ' ' + lastName + ' (' + fullName + ') = (' + __user__ + ')' ";

  // false all, to evaluate all conditions
  private static final String EXPR_CONDITION =
      "(title instanceof Contact || fullName == 'foo') || (__ref__ instanceof Title) || (__parent__ == 0.102) || (__self__ == __this__)";

  private void doTestSpeed(String expr) {
    final ScriptHelper helper = new JavaScriptScriptHelper(context());
    for (int i = 0; i < COUNT; i++) {
      Object result = helper.eval(expr);
      assertNotNull(result);
    }
  }

  private void doCastTest(int counter) {
    final ScriptHelper helper = new JavaScriptScriptHelper(context());

    Object actual = helper.eval("__parent__");
    assertTrue(actual instanceof Context);

    actual = helper.eval("__ref__");
    assertTrue(actual instanceof Contact);

    actual = helper.eval("__parent__.asType(Contact)");
    assertTrue(actual instanceof Contact);

    actual = helper.eval("__ref__.fullName");
    assertTrue(actual instanceof String);

    actual = helper.eval("__ref__.fullName + ' (" + counter + ")'");
  }

  @Test
  public void doCollectionTest() {
    final ScriptHelper helper = new JavaScriptScriptHelper(context());

    Object list = helper.eval("[1, 2, 3, 4]");
    assertNotNull(list);
    assertTrue(list instanceof List);
    assertEquals(4, ((List<?>) list).size());

    final Object map = helper.eval("({a: 1, b: 2})");
    assertNotNull(map);
    assertTrue(map instanceof Map);
    assertEquals(2, ((Map<?, ?>) map).size());
  }

  @Test
  public void doJsonTest() {
    final ScriptHelper helper = new JavaScriptScriptHelper(context());
    Object result = helper.eval("$attrs.nickName");
    assertTrue(result instanceof String);
    assertEquals("Some Name", result);

    result = helper.eval("orderAmount");
    assertTrue(result instanceof BigDecimal);
    assertEquals(0, new BigDecimal("1000.20").compareTo((BigDecimal) result));

    result = helper.eval("nickName");
    assertTrue(result instanceof String);
    assertEquals("Some Name", result);

    result = helper.eval("guardian");
    assertTrue(result instanceof Contact);

    result = helper.eval("guardian.fullName");
    assertNotNull(result);
  }

  @Test
  public void doJpaTest() {
    final ScriptHelper helper = new JavaScriptScriptHelper(context());
    final Object bean = helper.eval("doInJPA(em => __repo__(Contact).find(id))");

    assertNotNull(bean);
    assertTrue(bean instanceof Contact);
  }

  @Test
  public void test01_casts() {
    doCastTest(0);
  }

  // @Test
  public void test02_permgen() {
    int counter = 0;
    while (counter++ < 5000) {
      doCastTest(counter);
    }
  }

  @Test
  public void test10_warmup() {
    doTestSpeed(EXPR_INTERPOLATION);
  }

  @Test
  public void test11_interpolation() {
    doTestSpeed(EXPR_INTERPOLATION);
  }

  @Test
  public void test12_concat() {
    doTestSpeed(EXPR_CONCAT);
  }

  @Test
  public void test13_condition() {
    doTestSpeed(EXPR_CONDITION);
  }
}
