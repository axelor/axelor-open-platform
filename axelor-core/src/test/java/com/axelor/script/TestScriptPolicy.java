/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.JpaTestModule;
import com.axelor.common.ObjectUtils;
import com.axelor.db.mapper.Mapper;
import com.axelor.inject.Beans;
import com.axelor.meta.ActionExecutor;
import com.axelor.meta.schema.actions.ActionScript;
import com.axelor.meta.schema.actions.validate.validator.Notify;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
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
import com.axelor.test.db.Contact;
import com.axelor.test.db.repo.ContactRepository;
import com.google.inject.AbstractModule;
import com.google.inject.persist.Transactional;
import groovy.lang.Closure;
import java.util.List;
import java.util.Map;
import javax.script.SimpleBindings;
import org.codehaus.groovy.runtime.InvokerHelper;
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
    assertEquals(
        "InnerDefault",
        helper.eval(
            "__bean__(com.axelor.script.policy.AllowedByAnnotation.InnerDefault).myMethod()"));

    // Allowed by configuration
    assertEquals(
        "AllowedByConfiguration",
        helper.eval("com.axelor.script.policy.AllowedByConfiguration.myStaticMethod()"));
    assertEquals(
        "InnerAllowedByConfiguration",
        helper.eval(
            "com.axelor.script.policy.AllowedByConfiguration.InnerAllowed.myStaticMethod()"));

    // Denied by configuration
    assertThrows(
        IllegalArgumentException.class,
        () -> helper.eval("com.axelor.script.policy.DeniedByConfiguration.myStaticMethod()"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
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

    // Entity manager
    assertThrows(IllegalArgumentException.class, () -> helper.eval("com.axelor.db.JPA.em()"));
  }

  // Script policy evaluation differs from Groovy and EL:
  // - For inner classes, access is considered according to the top-level class
  // - Denied property access returns empty object instead of throwing exception
  @Test
  void testJavaScript() {
    try (JavaScriptScriptHelper helper = new JavaScriptScriptHelper(new SimpleBindings())) {
      // Allowed by annotation
      assertEquals(
          "AllowedByAnnotation",
          helper.eval("__bean__(com.axelor.script.policy.AllowedByAnnotation).myMethod()"));
      assertEquals(
          "InnerAllowedByAnnotation",
          helper.eval(
              "__bean__(com.axelor.script.policy.AllowedByAnnotation.InnerAllowed).myMethod()"));
      assertEquals(
          "InnerDefault",
          helper.eval(
              "__bean__(com.axelor.script.policy.AllowedByAnnotation.InnerDefault).myMethod()"));

      // Allowed by configuration
      assertEquals(
          "AllowedByConfiguration",
          helper.eval("com.axelor.script.policy.AllowedByConfiguration.myStaticMethod()"));
      assertEquals(
          "InnerAllowedByConfiguration",
          helper.eval(
              "com.axelor.script.policy.AllowedByConfiguration.InnerAllowed.myStaticMethod()"));

      // denied by configuration
      assertThrows(
          IllegalArgumentException.class,
          () -> helper.eval("com.axelor.script.policy.DeniedByConfiguration.myStaticMethod()"));
      assertThrows(
          IllegalArgumentException.class,
          () ->
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
          () ->
              helper.eval("com.axelor.script.policy.DeniedByDefault.InnerDenied.myStaticMethod()"));

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

      // Entity manager
      assertThrows(IllegalArgumentException.class, () -> helper.eval("com.axelor.db.JPA.em()"));
    }
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

  @Test
  void testGroovyActionScript() {
    var contact = ensureContact("John", "Doe", "john.doe@axelor.com");

    var action = new ActionScript();
    var script = new ActionScript.ActScript();
    script.setLanguage("groovy");
    action.setScript(script);

    var request = new ActionRequest();
    request.setModel(Contact.class.getName());
    request.setData(Map.of("context", Mapper.toMap(contact)));
    var handler = Beans.get(ActionExecutor.class).newActionHandler(request);

    // Test $request and $response
    script.setCode(
        """
        final firstName = $request.context.firstName;
        $response.setNotify("Hello, ${firstName}!");
        """);
    var response = (ActionResponse) action.execute(handler);
    assertEquals(Response.STATUS_SUCCESS, response.getStatus());

    @SuppressWarnings("unchecked")
    var data = ((List<Map<String, Object>>) response.getData()).get(0);
    @SuppressWarnings("unchecked")
    var notify = (Map<String, Object>) data.get(Notify.KEY);
    assertEquals("Hello, %s!".formatted(contact.getFirstName()), notify.get("message"));

    script.setTransactional(true);

    // Test $json
    script.setCode(
        """
        def record = $json.create("custom_model", [name: "Custom JSON Groovy"]);
        record = $json.save(record);
        $response.setNotify("Saved ${record.name}");
        """);
    response = (ActionResponse) action.execute(handler);
    assertEquals(Response.STATUS_SUCCESS, response.getStatus());

    @SuppressWarnings("unchecked")
    var jsonData = ((List<Map<String, Object>>) response.getData()).get(0);
    @SuppressWarnings("unchecked")
    var jsonNotify = (Map<String, Object>) jsonData.get(Notify.KEY);
    assertEquals("Saved Custom JSON Groovy", jsonNotify.get("message"));

    // Test $em
    script.setCode(
        """
        final found = $em.find(Contact, $request.context.id);
        $response.setNotify("Found ${found.firstName} ${found.lastName}");
        """);
    response = (ActionResponse) action.execute(handler);
    assertEquals(Response.STATUS_SUCCESS, response.getStatus());

    @SuppressWarnings("unchecked")
    var emData = ((List<Map<String, Object>>) response.getData()).get(0);
    @SuppressWarnings("unchecked")
    var emNotify = (Map<String, Object>) emData.get(Notify.KEY);
    assertEquals(
        "Found %s %s".formatted(contact.getFirstName(), contact.getLastName()),
        emNotify.get("message"));
  }

  @Test
  void testJavaScriptActionScript() {
    var contact = ensureContact("John", "Doe", "john.doe@axelor.com");

    var action = new ActionScript();
    var script = new ActionScript.ActScript();
    script.setLanguage("js");
    action.setScript(script);

    var request = new ActionRequest();
    request.setModel(Contact.class.getName());
    request.setData(Map.of("context", Mapper.toMap(contact)));
    var handler = Beans.get(ActionExecutor.class).newActionHandler(request);

    // Test $request and $response
    script.setCode(
        """
        const firstName = $request.context.firstName;
        $response.setNotify(`Hello, ${firstName}!`);
        """);
    var response = (ActionResponse) action.execute(handler);
    assertEquals(Response.STATUS_SUCCESS, response.getStatus());

    @SuppressWarnings("unchecked")
    var data = ((List<Map<String, Object>>) response.getData()).get(0);
    @SuppressWarnings("unchecked")
    var notify = (Map<String, Object>) data.get(Notify.KEY);
    assertEquals("Hello, %s!".formatted(contact.getFirstName()), notify.get("message"));

    script.setTransactional(true);

    // Test $json
    script.setCode(
        """
        let record = $json.create("custom_model", {"name": "Custom JSON JS"});
        record = $json.save(record);
        $response.setNotify(`Saved ${record.name}`);
        """);
    response = (ActionResponse) action.execute(handler);
    assertEquals(Response.STATUS_SUCCESS, response.getStatus());

    @SuppressWarnings("unchecked")
    var jsonData = ((List<Map<String, Object>>) response.getData()).get(0);
    @SuppressWarnings("unchecked")
    var jsonNotify = (Map<String, Object>) jsonData.get(Notify.KEY);
    assertEquals("Saved Custom JSON JS", jsonNotify.get("message"));

    // Test $em
    script.setCode(
        """
        const found = $em.find(Contact, $request.context.id);
        $response.setNotify(`Found ${found.firstName} ${found.lastName}`);
        """);
    response = (ActionResponse) action.execute(handler);
    assertEquals(Response.STATUS_SUCCESS, response.getStatus());

    @SuppressWarnings("unchecked")
    var emData = ((List<Map<String, Object>>) response.getData()).get(0);
    @SuppressWarnings("unchecked")
    var emNotify = (Map<String, Object>) emData.get(Notify.KEY);
    assertEquals(
        "Found %s %s".formatted(contact.getFirstName(), contact.getLastName()),
        emNotify.get("message"));
  }

  @Test
  void testGroovyGdkProcessExecutionIsDenied() {
    // A command that does not exist: nothing runs even if the policy fails to block the call.
    final String missingCmd = "axelor-nonexistent-binary-xyz";

    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    // Positive controls: denied via the receiver check.

    // Receiver is Runtime (denied class).
    assertBlockedByPolicy(helper, "Runtime.getRuntime().exec('" + missingCmd + "'); 1");

    // Constructor of a denied class.
    assertBlockedByPolicy(helper, "new ProcessBuilder('" + missingCmd + "').start(); 1");

    // The reported bypass: a GDK extension method dispatched on an allow-listed receiver.

    // Receiver is java.util.ArrayList (package java.util is allow-listed),
    // dispatched to ProcessGroovyMethods.execute(List).
    assertBlockedByPolicy(helper, "['" + missingCmd + "'].execute(); 1");

    // Receiver is java.lang.String (package java.lang is allow-listed),
    // dispatched to ProcessGroovyMethods.execute(String).
    assertBlockedByPolicy(helper, "'" + missingCmd + "'.execute(); 1");

    // Array receiver variant, dispatched to ProcessGroovyMethods.execute(String[]).
    assertBlockedByPolicy(helper, "(['" + missingCmd + "'] as String[]).execute(); 1");

    // Overload with env/dir arguments (null, so no denied type is constructed as a side effect),
    // dispatched to ProcessGroovyMethods.execute(List, String[], File).
    assertBlockedByPolicy(
        helper, "['" + missingCmd + "'].execute((String[]) null, (File) null); 1");

    // Spread (recv*.execute()) dispatches execute() on each element, so a receiver whose element
    // type (not the collection type) provides the GDK method must be caught too.
    assertBlockedByPolicy(helper, "(['" + missingCmd + "'] as Set)*.execute(); 1");
    assertBlockedByPolicy(helper, "[k: '" + missingCmd + "'].values()*.execute(); 1");
    assertBlockedByPolicy(helper, "(['" + missingCmd + "'] as String[])*.execute(); 1");

    // Other dispatch routes reaching the same GDK method, which the call-site checks cannot see:
    // they are caught by the SandboxGroovyMethods override of execute() itself.

    // Dynamic method name.
    assertBlockedByPolicy(helper, "'" + missingCmd + "'.\"${'exec' + 'ute'}\"(); 1");
    // invokeMethod with the method name as argument.
    assertBlockedByPolicy(helper, "'" + missingCmd + "'.invokeMethod('execute', null); 1");
    // Implicit call resolved on a closure delegate.
    assertBlockedByPolicy(helper, "'" + missingCmd + "'.with { execute() }; 1");
    assertBlockedByPolicy(helper, "'" + missingCmd + "'.tap { execute() }; 1");
    assertBlockedByPolicy(helper, "['" + missingCmd + "'].with { execute() }; 1");
    assertBlockedByPolicy(
        helper, "def c = { -> execute() }; c.delegate = '" + missingCmd + "'; c(); 1");
    // Spread over a receiver that is neither a Collection nor an array.
    assertBlockedByPolicy(helper, "['" + missingCmd + "'].iterator()*.execute(); 1");
  }

  // The sandbox must not depend on a name the script can rebind: a local variable or a closure
  // delegate named like the old policy checker variable must not replace the checks.
  @Test
  void testGroovyPolicyChecksCannotBeShadowed() {
    final String missingCmd = "axelor-nonexistent-binary-xyz";
    final String fake =
        "[check: { o -> o }, checkMethod: { o, n -> o }, checkSpread: { o, n -> o },"
            + " timeout: { -> null }]";

    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    assertBlockedByPolicy(
        helper,
        "def __$$policy = " + fake + "; Runtime.getRuntime().exec('" + missingCmd + "'); 1");
    assertBlockedByPolicy(
        helper,
        "[('__$$policy'): "
            + fake
            + "].with { Runtime.getRuntime().exec('"
            + missingCmd
            + "') }; 1");

    // A map delegate no longer hides the checks from the closure (it used to fail with a NPE).
    assertEquals("X", helper.eval("[name: 'x'].with { name.toUpperCase() }"));
  }

  // A spread (recv*.name) dispatches on each element of the receiver, not on the receiver itself,
  // so a denied class hidden in an allowed collection, iterator, etc. must be caught.
  @Test
  void testGroovySpreadElementsAreChecked() {
    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    // Spread method calls.
    assertBlockedByPolicy(helper, "[System]*.getenv('HOME')");
    assertBlockedByPolicy(helper, "([System] as Set)*.getenv('HOME')");
    assertBlockedByPolicy(helper, "[System].iterator()*.getenv('HOME')");

    // Spread property access.
    assertBlockedByPolicy(helper, "[System]*.env");
    assertBlockedByPolicy(helper, "[System].iterator()*.env");
    assertBlockedByPolicy(helper, "def l = [System]; l*.env");

    // Allowed spreads still work.
    assertEquals(List.of("1", "2"), helper.eval("[1, 2]*.toString()"));
    assertEquals(List.of("A", "B"), helper.eval("['a', 'b'].iterator()*.toUpperCase()"));
    assertEquals(List.of(1, 2), helper.eval("['a', 'bb']*.length()"));
    assertEquals(List.of("a"), helper.eval("[a: 1].entrySet()*.key"));
    assertEquals(null, helper.eval("def l = null; l*.size()"));
  }

  // Inside a closure, calls and properties without a receiver are resolved on its delegate (or
  // owner), which the receiver checks never see. A sandboxed closure must therefore never get a
  // delegate or owner the policy denies, however it is set.
  @Test
  void testGroovyClosureDelegateMustBeAllowed() {
    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());
    final String closure = "def c = { -> getenv('HOME') }; ";

    assertBlockedByPolicy(helper, closure + "c.delegate = System; c()");
    assertBlockedByPolicy(helper, closure + "c.setDelegate(System); c()");
    assertBlockedByPolicy(helper, closure + "c['delegate'] = System; c()");
    assertBlockedByPolicy(helper, closure + "c.setProperty('delegate', System); c()");
    assertBlockedByPolicy(helper, closure + "c.invokeMethod('setDelegate', System); c()");
    assertBlockedByPolicy(helper, closure + "c.\"${'setDel' + 'egate'}\"(System); c()");
    assertBlockedByPolicy(helper, closure + "c.rehydrate(System, null, null)()");
    assertBlockedByPolicy(helper, closure + "c.rehydrate(null, System, null)()");

    // Property access resolved on the delegate.
    assertBlockedByPolicy(
        helper, "def c = { -> env }; c.resolveStrategy = 1; c.delegate = System; c()");

    // Nested closure, resolved through its owner's delegate.
    assertBlockedByPolicy(
        helper, "def c = { -> { -> getenv('HOME') }() }; c.delegate = System; c()");
  }

  // Common closure usage must keep working with sandboxed closures.
  @Test
  void testGroovyClosuresStillWork() {
    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    // Script closures are sandboxed.
    assertTrue(helper.eval("{ -> 1 }") instanceof SandboxClosure);

    // Delegate set by GDK methods to an allowed receiver.
    assertEquals("ABC", helper.eval("'abc'.with { toUpperCase() }"));
    assertEquals("abc", helper.eval("'abc'.tap { toUpperCase() }"));
    assertEquals("X", helper.eval("[name: 'x'].with { name.toUpperCase() }"));
    assertEquals(3, helper.eval("[1, 2, 3].with { size() }"));

    // Delegate set explicitly to an allowed object.
    assertEquals(3, helper.eval("def c = { -> size() }; c.delegate = [1, 2, 3]; c()"));
    // resolveStrategy 1 is Closure.DELEGATE_FIRST (the Closure class itself is not allowed).
    assertEquals(
        "X",
        helper.eval(
            "def c = { -> name.toUpperCase() }; c.resolveStrategy = 1;"
                + " c.delegate = [name: 'x']; c()"));
    assertEquals(1, helper.eval("def c = { -> 1 }; c.rehydrate([1], null, null)()"));

    // Iteration and collection methods.
    assertEquals(10, helper.eval("[1, 2, 3].collect { it * 2 }.findAll { it > 2 }.sum()"));
    assertEquals("a1,b2", helper.eval("[a: 1, b: 2].collect { k, v -> k + v }.join(',')"));
    assertEquals(List.of(1, 2, 3), helper.eval("[3, 1, 2].sort { a, b -> a <=> b }"));
    assertEquals(6, helper.eval("[1, 2, 3].inject(0) { acc, v -> acc + v }"));
    assertEquals(
        Map.of(true, List.of(2), false, List.of(1, 3)),
        helper.eval("[1, 2, 3].groupBy { it % 2 == 0 }"));
    assertEquals(List.of(0, 1, 2), helper.eval("def l = []; 3.times { l << it }; l"));
    assertEquals(
        List.of("0:a", "1:b"),
        helper.eval("def l = []; ['a', 'b'].eachWithIndex { v, i -> l << i + ':' + v }; l"));

    // Nested closures, captured variables, default parameters, recursion.
    assertEquals(
        List.of(11, 12),
        helper.eval("def outer = { x -> [1, 2].collect { y -> x + y } }; outer(10)"));
    assertEquals(3, helper.eval("def n = 1; def inc = { -> n += 2 }; inc(); n"));
    assertEquals(5, helper.eval("def f = { x = 5 -> x }; f()"));
    assertEquals(
        120, helper.eval("def fact; fact = { n -> n <= 1 ? 1 : n * fact(n - 1) }; fact(5)"));

    // Closure owner and delegate are readable.
    assertEquals(true, helper.eval("def c = { -> delegate == owner }; c()"));

    // Closure returned to Java and called later.
    Closure<?> closure = (Closure<?>) helper.eval("{ a, b -> a + b }");
    assertEquals(3, closure.call(1, 2));
  }

  // Groovy categories and mixins expose methods of any class (e.g. System.getenv, GroovyShell)
  // on allow-listed receivers, so the receiver check does not apply to them.
  @Test
  void testGroovyCategoriesAndMixinsAreDenied() {
    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    assertBlockedByPolicy(helper, "use(System) { 'PATH'.getenv() }");
    assertBlockedByPolicy(helper, "def m = 'use'; \"$m\"(System) { 'PATH'.getenv() }");
    assertBlockedByPolicy(helper, "use([System]) { 'PATH'.getenv() }");
    assertBlockedByPolicy(helper, "String.mixin(GroovyShell); 'x'.evaluate('1 + 1')");
  }

  // A statically imported method is called without a receiver, so its class is checked when the
  // script is compiled.
  @Test
  void testGroovyStaticImportsAreChecked() {
    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    assertBlockedByPolicy(helper, "import static java.lang.System.getenv; getenv('PATH')");
    assertBlockedByPolicy(helper, "import static java.lang.System.getenv as env; env('PATH')");
    assertBlockedByPolicy(helper, "import static java.lang.System.*; getenv('PATH')");

    // Allowed classes can still be statically imported.
    assertEquals(2, helper.eval("import static java.lang.Math.max; max(1, 2)"));
  }

  // A closure created by a script stays sandboxed when called later, outside the evaluation.
  @Test
  void testGroovyClosureEscapingTheScriptIsSandboxed() {
    final String missingCmd = "axelor-nonexistent-binary-xyz";

    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());
    Closure<?> closure =
        (Closure<?>) helper.eval("{ -> '" + missingCmd + "'.\"${'exec' + 'ute'}\"() }");

    Throwable ex = assertThrows(RuntimeException.class, closure::call);
    assertTrue(hasPolicyException(ex), () -> "Expected a ScriptPolicyException, got: " + ex);
  }

  // The GDK overrides only deny sandboxed callers: trusted code keeps the GDK behavior.
  @Test
  void testGroovyGdkOverridesDoNotAffectTrustedCode() {
    final String missingCmd = "axelor-nonexistent-binary-xyz";

    // Reaches ProcessGroovyMethods.execute(String), which fails as the command does not exist.
    Throwable ex =
        assertThrows(
            Exception.class, () -> InvokerHelper.invokeMethod(missingCmd, "execute", null));
    assertFalse(hasPolicyException(ex), () -> "Expected no ScriptPolicyException, got: " + ex);
    assertTrue(
        String.valueOf(ex.getMessage()).contains("Cannot run program"),
        () -> "Expected the process to be started, got: " + ex);
  }

  // GDK IO/resource reads must stay denied. Today the receiver check already blocks them (URL/File
  // are not allow-listed); this guards that surface against a future change reopening file/URL
  // exfiltration. Targets are nonexistent paths, so an unblocked read only throws FileNotFound.
  @Test
  void testGroovyGdkResourceAccessIsDenied() {
    final String missingPath = "/axelor-nonexistent-path-xyz";

    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    // URL reads (receiver is java.net.URL): ResourceGroovyMethods/IOGroovyMethods.
    assertBlockedByPolicy(helper, "'file:" + missingPath + "'.toURL().text");
    assertBlockedByPolicy(helper, "'file:" + missingPath + "'.toURL().getText()");
    assertBlockedByPolicy(helper, "'file:" + missingPath + "'.toURL().bytes");
    assertBlockedByPolicy(helper, "'file:" + missingPath + "'.toURL().openStream(); 1");
    assertBlockedByPolicy(helper, "new URL('file:" + missingPath + "').text");

    // File read via coercion (receiver is java.io.File): ResourceGroovyMethods.getText(File).
    assertBlockedByPolicy(helper, "('" + missingPath + "' as File).text");
  }

  // A nested evaluation escapes the sandbox entirely: Script.evaluate(String) / Eval / new
  // GroovyShell() compile the nested code without the policy's AST customizers, so inside it even
  // Runtime/ProcessBuilder run unchecked.
  //
  // Eval.me and new GroovyShell() are denied by the receiver check (their class is not
  // allow-listed).
  // Script.evaluate(...) is not caught that way because the receiver is the trusted running script,
  // so SandboxScript, the base class of compiled scripts, overrides it to throw.
  @Test
  void testGroovyNestedEvaluationIsSandboxed() {
    final String missingCmd = "axelor-nonexistent-binary-xyz";

    ScriptHelper helper = new GroovyScriptHelper(new SimpleBindings());

    // Already denied by the receiver check (guards).
    assertBlockedByPolicy(helper, "Eval.me('1+1')");
    assertBlockedByPolicy(helper, "new GroovyShell(); 1");

    // Sandbox escape via Script.evaluate: the nested code would run unchecked and reach the process
    // layer; the evaluate() call itself is denied.
    assertBlockedByPolicy(
        helper, "evaluate(\"new ProcessBuilder('" + missingCmd + "').start(); 1\")");
    assertBlockedByPolicy(
        helper, "evaluate(\"Runtime.getRuntime().exec('" + missingCmd + "'); 1\")");
    assertBlockedByPolicy(helper, "evaluate(\"['" + missingCmd + "'].execute(); 1\")");

    // Other dispatch routes to evaluate().
    final String nested = "\"new ProcessBuilder('" + missingCmd + "').start(); 1\"";
    assertBlockedByPolicy(helper, "this.evaluate(" + nested + ")");
    assertBlockedByPolicy(helper, "invokeMethod('evaluate', " + nested + ")");
    assertBlockedByPolicy(helper, "this.\"${'eval' + 'uate'}\"(" + nested + ")");
    assertBlockedByPolicy(helper, "[1].with { evaluate(" + nested + ") }");
  }

  private void assertBlockedByPolicy(ScriptHelper helper, String code) {
    Throwable ex = assertThrows(RuntimeException.class, () -> helper.eval(code));
    assertTrue(
        hasPolicyException(ex),
        () ->
            "Expected the policy to deny ["
                + code
                + "] with a ScriptPolicyException, but it was not blocked. Got: "
                + ex);
  }

  private static boolean hasPolicyException(Throwable t) {
    for (Throwable c = t; c != null; c = c.getCause()) {
      if (c instanceof ScriptPolicyException) {
        return true;
      }
    }
    return false;
  }

  @Transactional
  Contact ensureContact(String firstName, String lastName, String email) {
    var repo = Beans.get(ContactRepository.class);
    var contact = repo.findByEmail(email);

    if (contact == null) {
      contact = new Contact(firstName, lastName);
      contact.setEmail(email);
      contact = repo.save(contact);
    }

    return contact;
  }
}
