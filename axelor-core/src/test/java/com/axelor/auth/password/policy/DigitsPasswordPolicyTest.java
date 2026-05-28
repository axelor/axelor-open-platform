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

public class DigitsPasswordPolicyTest {

  private final DigitsPasswordPolicy policy = new DefaultDigitsPasswordPolicy();

  @BeforeEach
  void setUp() {
    TestingHelpers.resetSettings();
  }

  @AfterEach
  void tearDown() {
    TestingHelpers.resetSettings();
  }

  @Test
  void passwordWithEnoughDigitsIsValid() {
    AppSettings.get()
        .getInternalProperties()
        .put(AvailableAppSettings.USER_PASSWORD_DIGITS_MIN, "2");
    assertNull(policy.validate(null, "abc12def"));
  }

  @Test
  void passwordWithTooFewDigitsIsInvalid() {
    AppSettings.get()
        .getInternalProperties()
        .put(AvailableAppSettings.USER_PASSWORD_DIGITS_MIN, "2");
    assertNotNull(policy.validate(null, "abc1def"));
  }

  @Test
  void passwordWithNoDigitsFailsDefaultMin() {
    assertNotNull(policy.validate(null, "nodigitshere"));
  }
}
