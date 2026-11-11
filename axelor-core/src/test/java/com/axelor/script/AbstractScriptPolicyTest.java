/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.JpaTestModule;
import com.axelor.inject.Beans;
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
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Shared setup and helpers of the scripting policy tests, one test class per script engine: {@link
 * TestGroovyScriptPolicy}, {@link TestJavaScriptScriptPolicy}, {@link TestELScriptPolicy}.
 */
@ExtendWith(GuiceExtension.class)
@GuiceModules({
  JpaTestModule.class,
  AbstractScriptPolicyTest.MyModule.class,
  AbstractScriptPolicyTest.MyModule2.class
})
abstract class AbstractScriptPolicyTest {

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

  static void assertBlockedByPolicy(ScriptHelper helper, String code) {
    // any throwable: a denial in a static initializer is reported as an ExceptionInInitializerError
    Throwable ex = assertThrows(Throwable.class, () -> helper.eval(code));
    assertTrue(
        hasPolicyException(ex),
        () ->
            "Expected the policy to deny ["
                + code
                + "] with a ScriptPolicyException, but it was not blocked. Got: "
                + ex);
  }

  static boolean hasPolicyException(Throwable t) {
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
