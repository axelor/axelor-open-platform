/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.common.ObjectUtils;
import com.axelor.db.mapper.Mapper;
import com.axelor.inject.Beans;
import com.axelor.meta.ActionExecutor;
import com.axelor.meta.schema.actions.ActionScript;
import com.axelor.meta.schema.actions.validate.validator.Notify;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
import com.axelor.test.db.Contact;
import java.util.List;
import java.util.Map;
import javax.script.SimpleBindings;
import org.junit.jupiter.api.Test;

/** Scripting policy of the JavaScript engine. */
class TestJavaScriptScriptPolicy extends AbstractScriptPolicyTest {

  // Script policy evaluation differs from Groovy and EL:
  // - For inner classes, access is considered according to the top-level class
  // - Denied property access returns empty object instead of throwing exception
  @Test
  void testPolicy() {
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
  void testActionScript() {
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
}
