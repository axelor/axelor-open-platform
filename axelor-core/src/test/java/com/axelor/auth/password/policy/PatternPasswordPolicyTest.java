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

public class PatternPasswordPolicyTest {

  @BeforeEach
  void setUp() {
    TestingHelpers.resetSettings();
  }

  @AfterEach
  void tearDown() {
    TestingHelpers.resetSettings();
  }

  @Test
  void policyIsInactiveWhenNoPatternValueConfigured() {
    PatternPasswordPolicy policy = new DefaultPatternPasswordPolicy();
    assertNull(policy.validate(null, "anypassword"));
  }

  @Test
  void passwordMatchingPatternIsValid() {
    AppSettings.get()
        .getInternalProperties()
        .put(AvailableAppSettings.USER_PASSWORD_PATTERN_VALUE, ".*[0-9].*");
    PatternPasswordPolicy policy = new DefaultPatternPasswordPolicy();
    assertNull(policy.validate(null, "password1"));
  }

  @Test
  void passwordNotMatchingPatternIsInvalid() {
    AppSettings.get()
        .getInternalProperties()
        .put(AvailableAppSettings.USER_PASSWORD_PATTERN_VALUE, ".*[0-9].*");
    PatternPasswordPolicy policy = new DefaultPatternPasswordPolicy();
    assertNotNull(policy.validate(null, "nodigitshere"));
  }
}
