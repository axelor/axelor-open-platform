/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.password.policy;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.axelor.TestingHelpers;
import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UpperCasePasswordPolicyTest {

  private final UpperCasePasswordPolicy policy = new DefaultUpperCasePasswordPolicy();

  @BeforeEach
  void setUp() {
    TestingHelpers.resetSettings();
  }

  @AfterEach
  void tearDown() {
    TestingHelpers.resetSettings();
  }

  @Test
  void passwordWithEnoughUpperCaseCharsIsValid() {
    AppSettings.get()
        .getInternalProperties()
        .put(AvailableAppSettings.USER_PASSWORD_UPPER_CASE_MIN, "2");
    assertNull(policy.validate(null, "abcABcdef"));
  }

  @Test
  void passwordWithTooFewUpperCaseCharsIsInvalid() {
    AppSettings.get()
        .getInternalProperties()
        .put(AvailableAppSettings.USER_PASSWORD_UPPER_CASE_MIN, "2");
    assertNotNull(policy.validate(null, "abcAbcdef"));
  }

  @Test
  void passwordWithNoUpperCaseCharsFailsDefaultMin() {
    assertNotNull(policy.validate(null, "alllowercase123"));
  }
}
