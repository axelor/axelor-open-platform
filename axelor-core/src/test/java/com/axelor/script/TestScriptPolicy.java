/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.JpaTestModule;
import com.axelor.common.ObjectUtils;
import com.axelor.script.policy.MyOtherService;
import com.axelor.script.policy.MyOtherServiceImpl;
import com.axelor.script.policy.MyOtherServiceImpl2;
import com.axelor.script.policy.MyService;
import com.axelor.script.policy.MyServiceImpl;
import com.axelor.script.policy.MyServiceImpl2;
import com.axelor.script.policy.MyYetAnotherService;
import com.axelor.script.policy.MyYetAnotherServiceImpl;
import com.axelor.test.GuiceExtension;
import com.axelor.test.GuiceModules;
import com.google.inject.AbstractModule;
import java.util.List;
import javax.script.SimpleBindings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GuiceExtension.class)
@GuiceModules({
  JpaTestModule.class,
  TestScriptPolicy.MyModule.class,
  TestScriptPolicy.MyModule2.class
})
class TestScriptPolicy {

  public static class MyModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(MyService.class).to(MyServiceImpl.class);
      bind(MyOtherService.class).to(MyOtherServiceImpl.class);
      bind(MyYetAnotherService.class).to(MyYetAnotherServiceImpl.class);
    }
  }

  public static class MyModule2 extends AbstractModule {
    @Override
    protected void configure() {
      bind(MyServiceImpl.class).to(MyServiceImpl2.class);
      bind(MyOtherServiceImpl.class).to(MyOtherServiceImpl2.class);
    }
  }

  @Test
  void testGroovy() {
    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    // Allowed by annotation
    assertEquals(
        "AllowedByAnnotation",
        helper.eval("__bean__(com.axelor.script.policy.AllowedByAnnotation).myMethod()"));
    assertEquals(
        "InnerAllowedByAnnotation",
        helper.eval(
            "__bean__(com.axelor.script.policy.AllowedByAnnotation.InnerAllowed).myMethod()"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            helper.eval(
                "__bean__(com.axelor.script.policy.AllowedByAnnotation.InnerDenied).myMethod()"));

    // Allowed by configuration
    assertEquals(
        "AllowedByConfiguration",
        helper.eval("com.axelor.script.policy.AllowedByConfiguration.myStaticMethod()"));
    assertEquals(
        "InnerAllowedByConfiguration",
        helper.eval(
            "com.axelor.script.policy.AllowedByConfiguration.InnerAllowed.myStaticMethod()"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            helper.eval(
                "com.axelor.script.policy.AllowedByConfiguration.InnerDenied.myStaticMethod()"));

    // Denied by configuration
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("com.axelor.script.policy.DeniedByConfiguration.myStaticMethod()"));
    assertEquals(
        "InnerAllowedByAnnotation",
        helper.eval(
            "com.axelor.script.policy.DeniedByConfiguration.InnerAllowed.myStaticMethod()"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            helper.eval(
                "com.axelor.script.policy.DeniedByConfiguration.InnerDenied.myStaticMethod()"));

    // Denied by default
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("com.axelor.script.policy.DeniedByDefault.myStaticMethod()"));
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("com.axelor.script.policy.DeniedByDefault.InnerDenied.myStaticMethod()"));

    assertEquals(
        "SubAllowedByAnnotation",
        helper.eval("__bean__(com.axelor.script.policy.SubAllowedByAnnotation).myMethod()"));

    assertEquals(
        "SubAllowedByConfiguration",
        helper.eval("com.axelor.script.policy.SubAllowedByConfiguration.myStaticMethod()"));

    // Service
    assertEquals(
        "Hello, World!", helper.eval("__bean__(com.axelor.script.policy.MyService).myMethod()"));
    assertEquals(
        "myYetAnotherMethod",
        helper.eval("__bean__(com.axelor.script.policy.MyService).myYetAnotherMethod()"));
    assertEquals(
        2, helper.eval("__bean__(com.axelor.script.policy.MyOtherService).myOtherMethod()"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            helper.eval(
                "__bean__(com.axelor.script.policy.MyYetAnotherService).myYetAnotherMethod()"));

    // Property access
    assertEquals(
        "AllowedByAnnotation",
        helper.eval("__bean__(com.axelor.script.policy.AllowedByAnnotation).myValue"));
    assertEquals(
        "AllowedByConfiguration",
        helper.eval("com.axelor.script.policy.AllowedByConfiguration.myStaticValue"));
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("com.axelor.script.policy.DeniedByConfiguration.myStaticValue"));
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("__bean__(com.axelor.script.policy.DeniedByConfiguration).myValue"));

    // Constant access
    assertEquals("myConstant", helper.eval("com.axelor.script.policy.MyService.MY_CONSTANT"));
    assertEquals(
        List.of("hello", "world"),
        helper.eval("com.axelor.script.policy.MyService.MY_CONSTANT_LIST"));
    assertEquals(
        "myOtherConstant", helper.eval("com.axelor.script.policy.MyOtherService.MY_CONSTANT"));
    assertEquals(
        List.of("hello", "world"),
        helper.eval("com.axelor.script.policy.MyOtherService.MY_CONSTANT_LIST"));
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("com.axelor.script.policy.MyYetAnotherService.MY_CONSTANT"));
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("com.axelor.script.policy.MyYetAnotherService.MY_CONSTANT_LIST"));

    // Service returning instance of denied class
    assertDoesNotThrow(
        () ->
            helper.eval(
                """
                var myService = __bean__(com.axelor.script.policy.MyService);
                var a = myService.myYetAnotherMethod();

                if (a != "myYetAnotherMethod") {
                  throw new Exception("Should be allowed");
                }

                var myYetAnotherService = myService.getMyYetAnotherService();

                try {
                  var b = myYetAnotherService.MY_CONSTANT;
                  if (b?.length() > 0) {
                    throw new Exception("Should be denied");
                  }
                } catch (com.axelor.script.ScriptPolicyException e) {
                  // Denied as expected
                }
                """));
  }

  // Script policy evaluation differs from Groovy and EL:
  // - For inner classes, access is considered according to the top-level class
  // - Denied property access returns empty object instead of throwing exception
  @Test
  void testJavaScript() {
    ScriptHelper helper = new JavaScriptScriptHelper(new SimpleBindings());

    // Allowed by annotation
    assertEquals(
        "AllowedByAnnotation",
        helper.eval("__bean__(com.axelor.script.policy.AllowedByAnnotation).myMethod()"));
    assertEquals(
        "InnerAllowedByAnnotation",
        helper.eval(
            "__bean__(com.axelor.script.policy.AllowedByAnnotation.InnerAllowed).myMethod()"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            helper.eval(
                "__bean__(com.axelor.script.policy.AllowedByAnnotation.InnerDenied).myMethod()"));

    // Allowed by configuration
    assertEquals(
        "AllowedByConfiguration",
        helper.eval("com.axelor.script.policy.AllowedByConfiguration.myStaticMethod()"));
    assertEquals(
        "InnerAllowedByConfiguration",
        helper.eval(
            "com.axelor.script.policy.AllowedByConfiguration.InnerAllowed.myStaticMethod()"));
    // assertThrows(
    //     IllegalArgumentException.class,
    //     () ->
    //         helper.eval(
    //             "com.axelor.script.policy.AllowedByConfiguration.InnerDenied.myStaticMethod()"));

    // denied by configuration
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("com.axelor.script.policy.DeniedByConfiguration.myStaticMethod()"));
    // assertEquals(
    //     "InnerAllowedByAnnotation",
    //     helper.eval(
    //         "com.axelor.script.policy.DeniedByConfiguration.InnerAllowed.myStaticMethod()"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            helper.eval(
                "com.axelor.script.policy.DeniedByConfiguration.InnerDenied.myStaticMethod()"));

    // Denied by default
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("com.axelor.script.policy.DeniedByDefault.myStaticMethod()"));
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("com.axelor.script.policy.DeniedByDefault.InnerDenied.myStaticMethod()"));

    assertEquals(
        "SubAllowedByAnnotation",
        helper.eval("__bean__(com.axelor.script.policy.SubAllowedByAnnotation).myMethod()"));

    assertEquals(
        "SubAllowedByConfiguration",
        helper.eval("com.axelor.script.policy.SubAllowedByConfiguration.myStaticMethod()"));

    // Service method calls
    assertEquals(
        "Hello, World!", helper.eval("__bean__(com.axelor.script.policy.MyService).myMethod()"));
    assertEquals(
        "myYetAnotherMethod",
        helper.eval("__bean__(com.axelor.script.policy.MyService).myYetAnotherMethod()"));
    assertEquals(
        2, helper.eval("__bean__(com.axelor.script.policy.MyOtherService).myOtherMethod()"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            helper.eval(
                "__bean__(com.axelor.script.policy.MyYetAnotherService).myYetAnotherMethod()"));

    // Property access
    assertEquals(
        "AllowedByAnnotation",
        helper.eval("__bean__(com.axelor.script.policy.AllowedByAnnotation).myValue"));
    assertEquals(
        "AllowedByConfiguration",
        helper.eval("com.axelor.script.policy.AllowedByConfiguration.myStaticValue"));
    assertTrue(
        ObjectUtils.isEmpty(
            helper.eval("com.axelor.script.policy.DeniedByConfiguration.myStaticValue")));
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("__bean__(com.axelor.script.policy.DeniedByConfiguration).myValue"));

    // Constant access
    assertEquals("myConstant", helper.eval("com.axelor.script.policy.MyService.MY_CONSTANT"));
    assertEquals(
        List.of("hello", "world"),
        helper.eval("com.axelor.script.policy.MyService.MY_CONSTANT_LIST"));
    assertEquals(
        "myOtherConstant", helper.eval("com.axelor.script.policy.MyOtherService.MY_CONSTANT"));
    assertEquals(
        List.of("hello", "world"),
        helper.eval("com.axelor.script.policy.MyOtherService.MY_CONSTANT_LIST"));
    assertTrue(
        ObjectUtils.isEmpty(
            helper.eval("com.axelor.script.policy.MyYetAnotherService.MY_CONSTANT")));
    assertTrue(
        ObjectUtils.isEmpty(
            helper.eval("com.axelor.script.policy.MyYetAnotherService.MY_CONSTANT_LIST")));

    // Service returning instance of denied class
    assertDoesNotThrow(
        () ->
            helper.eval(
                """
                const myService = __bean__(com.axelor.script.policy.MyService);
                const a = myService.myYetAnotherMethod();

                if (a !== "myYetAnotherMethod") {
                  throw new Error("Should be allowed");
                }

                const myYetAnotherService = myService.getMyYetAnotherService();

                try {
                  const b = myYetAnotherService.MY_CONSTANT;
                  if (b?.length > 0) {
                    throw new Exception("Should be denied");
                  }
                } catch (e) {
                  // Denied as expected
                }
                """));
  }

  @Test
  void testEL() {
    ScriptHelper helper = new ELScriptHelper(new SimpleBindings());

    // Allowed by annotation
    assertEquals(
        "AllowedByAnnotation",
        helper.eval("__bean__(T('com.axelor.script.policy.AllowedByAnnotation')).myMethod()"));
    assertEquals(
        "InnerAllowedByAnnotation",
        helper.eval(
            "__bean__(T('com.axelor.script.policy.AllowedByAnnotation$InnerAllowed')).myMethod()"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            helper.eval(
                "__bean__(T('com.axelor.script.policy.AllowedByAnnotation$InnerDenied')).myMethod()"));

    // Allowed by configuration
    assertEquals(
        "AllowedByConfiguration",
        helper.eval("T('com.axelor.script.policy.AllowedByConfiguration').myStaticMethod()"));
    assertEquals(
        "InnerAllowedByConfiguration",
        helper.eval(
            "T('com.axelor.script.policy.AllowedByConfiguration$InnerAllowed').myStaticMethod()"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            helper.eval(
                "T('com.axelor.script.policy.AllowedByConfiguration$InnerDenied').myStaticMethod()"));

    // Denied by configuration
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("T('com.axelor.script.policy$DeniedByConfiguration').myStaticMethod()"));
    assertEquals(
        "InnerAllowedByAnnotation",
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
  }
}
