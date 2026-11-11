/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Package patterns of the scripting policy (allowed/denied packages). */
class TestScriptPolicyMatch {

  @Test
  void exactPatternMatchesOnlyThatPackage() {
    assertTrue(ScriptPolicy.match("java.util", "java.util"));

    assertFalse(ScriptPolicy.match("java.util", "java.util.concurrent"));
    assertFalse(ScriptPolicy.match("java.util", "java"));
    assertFalse(ScriptPolicy.match("java.util", "java.utilx"));
    assertFalse(ScriptPolicy.match("java.util", ""));
  }

  @Test
  void wildcardPatternMatchesPackageAndImmediateSubPackages() {
    assertTrue(ScriptPolicy.match("java.time.*", "java.time"));
    assertTrue(ScriptPolicy.match("java.time.*", "java.time.format"));
    assertTrue(ScriptPolicy.match("java.time.*", "java.time.zone"));

    // deeper sub-packages
    assertFalse(ScriptPolicy.match("java.time.*", "java.time.format.internal"));
    // other packages sharing a prefix
    assertFalse(ScriptPolicy.match("java.time.*", "java.timex"));
    assertFalse(ScriptPolicy.match("java.time.*", "java.timex.format"));
    assertFalse(ScriptPolicy.match("java.time.*", "java"));
    assertFalse(ScriptPolicy.match("java.time.*", "com.example"));
  }

  // The previous implementation stripped the prefix and counted the remaining segments, so the
  // default package and any one-word package matched every wildcard pattern.
  @Test
  void wildcardPatternDoesNotMatchDefaultOrOneWordPackages() {
    assertFalse(ScriptPolicy.match("java.time.*", ""));
    assertFalse(ScriptPolicy.match("java.time.*", "foo"));
    assertFalse(ScriptPolicy.match("com.axelor.sale.service.*", ""));
    assertFalse(ScriptPolicy.match("com.axelor.sale.service.*", "evil"));
  }
}
