/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.rpc.Context;
import com.axelor.test.db.Contact;
import com.axelor.test.db.repo.ContactRepository;
import com.axelor.test.db.repo.CurrencyRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestGroovy extends ScriptTest {

  private static final int COUNT = 1000;

  private static final String EXPR_INTERPOLATION =
      "\"(${title.name}) = $firstName $lastName ($fullName) = ($__user__)\"";

  private static final String EXPR_CONCAT =
      "'(' + title.name + ') = ' + firstName + ' ' + lastName + ' (' + fullName + ') = (' + __user__ + ')' ";

  private static final String EXPR_ELVIS =
      "'(' + __this__?.title?.name + ') = ' + __this__?.firstName + ' ' + __this__?.lastName + ' (' + __this__?.fullName + ') = (' + __user__ + ')' ";

  // false all, to evaluate all conditions
  private static final String EXPR_CONDITION =
      "(title instanceof Contact || fullName == 'foo') || (__ref__ instanceof Title) || (__parent__ == 0.102) || (__self__ == __this__)";

  @Test
  public void testImport() {
    GroovyScriptHelper helper = new GroovyScriptHelper(context());
    Object actual;

    // Not need of FQN
    actual = helper.eval("LocalDate.of(2020, 5, 22)");
    assertEquals(LocalDate.of(2020, 5, 22), actual);

    // __repo__
    actual = helper.eval("__repo__(Contact)");
    assertTrue(actual instanceof ContactRepository);

    // Currency is also part of java.util package. When used
    // with __repo__ it should resolve the Model class
    actual = helper.eval("__repo__(Currency)");
    assertTrue(actual instanceof CurrencyRepository);

    actual = helper.eval("Currency");
    assertTrue(((Class) actual).isAssignableFrom(java.util.Currency.class));
  }

  @Test
  public void testEvalCast() {
    GroovyScriptHelper helper = new GroovyScriptHelper(context());
    Object actual;

    actual = helper.eval("__parent__");
    assertTrue(actual instanceof Context);

    actual = helper.eval("__ref__");
    assertTrue(actual instanceof Contact);

    actual = helper.eval("__parent__ as Contact");
    assertTrue(actual instanceof Contact);

    actual = helper.eval("(__ref__ as Contact).fullName");
    assertTrue(actual instanceof String);

    actual = helper.eval("(__ref__ as Contact).fullName + ' (" + 0 + ")'");
    assertEquals("Mr. John Smith (0)", actual);
  }

  @Test
  public void testInterpolation() {
    GroovyScriptHelper helper = new GroovyScriptHelper(context());
    Object result = helper.eval(EXPR_INTERPOLATION);

    assertEquals("(Mrs.) = John NAME (Mrs. John NAME) = (null)", result.toString());
  }

  @Test
  public void testConcat() {
    GroovyScriptHelper helper = new GroovyScriptHelper(context());
    Object result = helper.eval(EXPR_CONCAT);

    assertEquals("(Mrs.) = John NAME (Mrs. John NAME) = (null)", result);
  }

  @Test
  public void testElvis() {
    GroovyScriptHelper helper = new GroovyScriptHelper(context());
    Object result = helper.eval(EXPR_ELVIS);

    assertEquals("(Mrs.) = John NAME (Mrs. John NAME) = (null)", result);
  }

  @Test
  public void testCondition() {
    GroovyScriptHelper helper = new GroovyScriptHelper(context());
    Object result = helper.eval(EXPR_CONDITION);

    assertTrue((Boolean) result);
  }

  @Test
  public void testEnum() {
    GroovyScriptHelper helper = new GroovyScriptHelper(context());
    Object result = helper.eval("EnumStatusNumber.ONE == contactStatus");

    assertTrue((Boolean) result);
  }

  @Test
  void testIntersect() {
    GroovyScriptHelper helper = new GroovyScriptHelper(context());
    Object result = helper.eval("[-2, -3].intersect([1, 2], (a, b) -> a.abs() <=> b.abs())");

    // Fixed in Groovy 4.0.0: https://issues.apache.org/jira/browse/GROOVY-10275
    assertEquals(List.of(-2), result);
  }

  @Test
  void testNegativeZero() {
    GroovyScriptHelper helper = new GroovyScriptHelper(context());
    Object result = helper.eval("def a = -0.0f, b = 0.0f; a != b");

    // Fixed in Groovy 4.0.0: https://issues.apache.org/jira/browse/GROOVY-9797
    assertTrue((Boolean) result);
  }

  @Test
  void testReferentialTransparency() {
    GroovyScriptHelper helper = new GroovyScriptHelper(context());
    Object result =
        helper.eval(
            """
            def a = ['a', 'b'] as String[], b = ['c', 'd'] as String[]
            def c = a + b
            c instanceof String[]
            """);

    // Fixed in Groovy 4.0.0, 3.0.21: https://issues.apache.org/jira/browse/GROOVY-6837
    assertTrue((Boolean) result);
  }

  @Test
  public void testSecurity() {
    GroovyScriptHelper helper = new GroovyScriptHelper(context());

    // classes from java.lang should be allowed
    assertTrue((Boolean) helper.eval("Boolean.TRUE"));

    // but java.lang.{System,Process,Thread} are not allowed
    assertThrows(IllegalArgumentException.class, () -> helper.eval("System.currentTimeMillis()"));
    assertThrows(IllegalArgumentException.class, () -> helper.eval("System.exit(-1)"));
    assertThrows(IllegalArgumentException.class, () -> helper.eval("Thread.sleep(1000)"));
    assertThrows(IllegalArgumentException.class, () -> helper.eval("Thread.sleep(1000)"));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            helper.eval("new ProcessBuilder().command('ls', '-l').inheritIO().start().waitFor()"));

    // allow models
    assertNotNull(helper.eval("__repo__(Title).all().fetchOne().name"));

    // app settings not allowed
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("com.axelor.app.AppSettings.get().get('db.test.url')"));

    // app settings not allowed through __config__
    assertNull(helper.eval("__config__.get('db.test.url')"));

    // only custom settings helper allowed
    assertNull(helper.eval("__config__.get('application.mode')"));
    assertNotNull(
        helper.eval("__bean__(com.axelor.script.policy.ScriptAppSettings).getApplicationMode()"));

    // also keys prefixed with `context.*` (without `context.`)
    assertNotNull(helper.eval("__config__.get('string')"));
    assertNotNull(helper.eval("__config__.string"));

    // should work with safe navigation operator
    assertNotNull(
        helper.eval(
            "__bean__(com.axelor.script.policy.ScriptAppSettings).getApplicationMode()?.length()"));

    // trying to access a file
    assertThrows(IllegalArgumentException.class, () -> helper.eval("new java.io.File('/tmp')"));
    assertThrows(
        IllegalArgumentException.class, () -> helper.eval("java.nio.file.Paths.get('/tmp')"));

    // even try with reflection
    assertThrows(
        IllegalArgumentException.class,
        () ->
            helper.eval(
                "String.class.forName('java.io.File').getConstructor(String.class).newInstance('/some/file')"));

    // GString template
    assertEquals(
        "dev",
        String.valueOf(
            helper.eval(
                "\"${__bean__(com.axelor.script.policy.ScriptAppSettings).getApplicationMode()}\"")),
        "Should allow custom helper in GString");
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("\"${com.axelor.app.AppSettings.get().get('db.test.url')}\""),
        "Should not allow unrestricted AppSettings in GString");

    // Closure
    assertEquals(
        "dev",
        String.valueOf(
            helper.eval(
                "{\"${__bean__(com.axelor.script.policy.ScriptAppSettings).getApplicationMode()}\"}()")),
        "Should allow custom helper in Closure");
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("{\"${com.axelor.app.AppSettings.get().get('db.test.url')}\"}()"),
        "Should not allow unrestricted AppSettings in Closure");
  }

  @Test
  public void testTimeout() {
    GroovyScriptHelper helper = new GroovyScriptHelper(context()).withTimeout(100);
    assertThrows(
        IllegalArgumentException.class, () -> helper.eval("while (true) { println('hello!') }"));
  }
}
