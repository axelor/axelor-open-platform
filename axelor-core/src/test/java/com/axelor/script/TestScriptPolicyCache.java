/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.script.policy.AllowedByAnnotation;
import com.axelor.script.policy.AllowedByConfiguration;
import com.axelor.script.policy.DeniedByConfiguration;
import com.axelor.script.policy.DeniedByDefault;
import com.axelor.script.policy.MyService;
import com.axelor.script.policy.MyServiceImpl;
import com.axelor.script.policy.SubAllowedByAnnotation;
import com.axelor.script.policy.SubAllowedByConfiguration;
import com.axelor.test.db.Contact;
import com.axelor.test.db.repo.ContactRepository;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The scripting policy caches its answer per class: it must not change any answer. */
class TestScriptPolicyCache extends AbstractScriptPolicyTest {

  /** Classes covering each rule of the policy, so that a cache bug on any rule would show up. */
  private static final List<Class<?>> CLASSES =
      List.of(
          // allowed packages, arrays, nested classes
          String.class,
          String[].class,
          int[].class,
          ArrayList.class,
          HashMap.class,
          new HashMap<>(Map.of(1, 1)).entrySet().iterator().next().getClass(),
          LocalDate.class,
          DateTimeFormatter.class,
          BigDecimal.class,
          Locale.class,
          Locale.Category.class,
          // denied classes
          Formatter.class,
          ProcessBuilder.class,
          Runtime.class,
          Thread.class,
          Thread[].class,
          // not allowed
          File.class,
          File[].class,
          DeniedByDefault.class,
          // models and repositories
          Contact.class,
          ContactRepository.class,
          // by annotation and configuration, with subclasses
          AllowedByAnnotation.class,
          SubAllowedByAnnotation.class,
          AllowedByConfiguration.class,
          SubAllowedByConfiguration.class,
          DeniedByConfiguration.class,
          MyService.class,
          MyServiceImpl.class);

  private final ScriptPolicy policy = ScriptPolicy.getInstance();

  /**
   * The cached answer is the one the policy computes: on the first lookup (which fills the cache)
   * and on the next one (read from the cache), for each class.
   */
  @Test
  void cachedAnswerIsTheComputedOne() {
    for (Class<?> type : CLASSES) {
      boolean computed = policy.computeAllowed(type);
      assertEquals(computed, policy.allowed(type), () -> "First lookup of " + type.getName());
      assertEquals(computed, policy.allowed(type), () -> "Cached lookup of " + type.getName());
    }
  }

  /**
   * Lookups from several threads at the same time (as with concurrent requests) always return the
   * computed answer.
   */
  @Test
  void concurrentLookupsAgree() {
    CLASSES.parallelStream()
        .forEach(
            type -> {
              boolean computed = policy.computeAllowed(type);
              for (int i = 0; i < 100; i++) {
                assertEquals(computed, policy.allowed(type), type.getName());
              }
            });
  }

  /**
   * The answers themselves are right: the tests above only compare the cache with the computation,
   * so they would pass if both were wrong. Also checks that {@link ScriptPolicy#check(Class)},
   * which relies on the cache, keeps rejecting a denied class on every call.
   */
  @Test
  void expectedAnswers() {
    assertTrue(policy.allowed(String.class));
    assertTrue(policy.allowed(String[].class));
    assertTrue(policy.allowed(int[].class));
    assertTrue(policy.allowed(DateTimeFormatter.class));
    assertTrue(policy.allowed(Contact.class));
    assertTrue(policy.allowed(AllowedByAnnotation.class));
    assertTrue(policy.allowed(SubAllowedByAnnotation.class));
    assertTrue(policy.allowed(AllowedByConfiguration.class));

    assertFalse(policy.allowed(Formatter.class));
    assertFalse(policy.allowed(Thread.class));
    assertFalse(policy.allowed(Thread[].class));
    assertFalse(policy.allowed(File.class));
    assertFalse(policy.allowed(DeniedByDefault.class));
    assertFalse(policy.allowed(DeniedByConfiguration.class));

    // check() relies on the same answer
    assertEquals(String.class, policy.check(String.class));
    assertThrows(ScriptPolicyException.class, () -> policy.check(Thread.class));
    assertThrows(ScriptPolicyException.class, () -> policy.check(Thread.class));
  }
}
