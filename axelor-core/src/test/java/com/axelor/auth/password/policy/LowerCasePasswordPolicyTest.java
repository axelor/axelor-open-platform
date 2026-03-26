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

public class LowerCasePasswordPolicyTest {

  private final LowerCasePasswordPolicy policy = new DefaultLowerCasePasswordPolicy();

  @BeforeEach
  void setUp() {
    TestingHelpers.resetSettings();
  }

  @AfterEach
  void tearDown() {
    TestingHelpers.resetSettings();
  }

  @Test
  void passwordWithEnoughLowerCaseCharsIsValid() {
    AppSettings.get()
        .getInternalProperties()
        .put(AvailableAppSettings.USER_PASSWORD_LOWER_CASE_MIN, "2");
    assertNull(policy.validate(null, "ABCabcDEF"));
  }

  @Test
  void passwordWithTooFewLowerCaseCharsIsInvalid() {
    AppSettings.get()
        .getInternalProperties()
        .put(AvailableAppSettings.USER_PASSWORD_LOWER_CASE_MIN, "2");
    assertNotNull(policy.validate(null, "ABCaBCDEF"));
  }

  @Test
  void passwordWithNoLowerCaseCharsFailsDefaultMin() {
    assertNotNull(policy.validate(null, "ALLUPPERCASE123"));
  }
}
