/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import javax.script.SimpleBindings;
import org.junit.jupiter.api.Test;

/** Scripting policy of the Expression Language engine. */
class TestELScriptPolicy extends AbstractScriptPolicyTest {

  @Test
  void testPolicy() {
    ScriptHelper helper = new ELScriptHelper(new SimpleBindings());

    // Allowed by annotation
    assertEquals(
        "AllowedByAnnotation",
        helper.eval("__bean__(T('com.axelor.script.policy.AllowedByAnnotation')).myMethod()"));
    assertEquals(
        "InnerAllowedByAnnotation",
        helper.eval(
            "__bean__(T('com.axelor.script.policy.AllowedByAnnotation$InnerAllowed')).myMethod()"));
    assertEquals(
        "InnerDefault",
        helper.eval(
            "__bean__(T('com.axelor.script.policy.AllowedByAnnotation$InnerDefault')).myMethod()"));

    // Allowed by configuration
    assertEquals(
        "AllowedByConfiguration",
        helper.eval("T('com.axelor.script.policy.AllowedByConfiguration').myStaticMethod()"));
    assertEquals(
        "InnerAllowedByConfiguration",
        helper.eval(
            "T('com.axelor.script.policy.AllowedByConfiguration$InnerAllowed').myStaticMethod()"));

    // Denied by configuration
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("T('com.axelor.script.policy$DeniedByConfiguration').myStaticMethod()"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            helper.eval(
                "T('com.axelor.script.policy.DeniedByConfiguration$InnerAllowed').myStaticMethod()"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            helper.eval(
                "T('com.axelor.script.policy.DeniedByConfiguration$InnerDenied').myStaticMethod()"));

    // Denied by default
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("T('com.axelor.script.policy.DeniedByDefault').myStaticMethod()"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            helper.eval(
                "T('com.axelor.script.policy.DeniedByDefault$InnerDenied').myStaticMethod()"));

    assertEquals(
        "SubAllowedByAnnotation",
        helper.eval("__bean__(T('com.axelor.script.policy.SubAllowedByAnnotation')).myMethod()"));

    assertEquals(
        "SubAllowedByConfiguration",
        helper.eval("T('com.axelor.script.policy.SubAllowedByConfiguration').myStaticMethod()"));

    // Service
    assertEquals(
        "Hello, World!",
        helper.eval("__bean__(T('com.axelor.script.policy.MyService')).myMethod()"));
    assertEquals(
        "myYetAnotherMethod",
        helper.eval("__bean__(T('com.axelor.script.policy.MyService')).myYetAnotherMethod()"));
    assertEquals(
        2, helper.eval("__bean__(T('com.axelor.script.policy.MyOtherService')).myOtherMethod()"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            helper.eval(
                "__bean__(T('com.axelor.script.policy.MyYetAnotherService')).myYetAnotherMethod()"));

    // Property access
    assertEquals(
        "AllowedByAnnotation",
        helper.eval("__bean__(T('com.axelor.script.policy.AllowedByAnnotation')).myValue"));
    assertEquals(
        "AllowedByConfiguration",
        helper.eval("T('com.axelor.script.policy.AllowedByConfiguration').myStaticValue"));
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("T('com.axelor.script.policy.DeniedByConfiguration').myStaticValue"));
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("__bean__(T('com.axelor.script.policy.DeniedByConfiguration')).myValue"));

    // Constant access
    assertEquals("myConstant", helper.eval("T('com.axelor.script.policy.MyService').MY_CONSTANT"));
    assertEquals(
        List.of("hello", "world"),
        helper.eval("T('com.axelor.script.policy.MyService').MY_CONSTANT_LIST"));
    assertEquals(
        "myOtherConstant", helper.eval("T('com.axelor.script.policy.MyOtherService').MY_CONSTANT"));
    assertEquals(
        List.of("hello", "world"),
        helper.eval("T('com.axelor.script.policy.MyOtherService').MY_CONSTANT_LIST"));
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("T('com.axelor.script.policy.MyYetAnotherService').MY_CONSTANT"));
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("T('com.axelor.script.policy.MyYetAnotherService').MY_CONSTANT_LIST"));

    // Service returning instance of denied class
    assertThrows(
        IllegalArgumentException.class,
        () ->
            helper.eval(
                "__bean__(T('com.axelor.script.policy.MyService')).getMyYetAnotherService().MY_CONSTANT"));

    // Entity manager
    assertThrows(IllegalArgumentException.class, () -> helper.eval("T('com.axelor.db.JPA').em()"));
  }
}
