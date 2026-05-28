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

public class LengthPasswordPolicyTest {

  private final LengthPasswordPolicy policy = new DefaultLengthPasswordPolicy();

  @BeforeEach
  void setUp() {
    TestingHelpers.resetSettings();
  }

  @AfterEach
  void tearDown() {
    TestingHelpers.resetSettings();
  }

  @Test
  void passwordMeetingMinLengthIsValid() {
    AppSettings.get()
        .getInternalProperties()
        .put(AvailableAppSettings.USER_PASSWORD_LENGTH_MIN, "8");
    assertNull(policy.validate(null, "abcdefgh"));
  }

  @Test
  void passwordShorterThanMinLengthIsInvalid() {
    AppSettings.get()
        .getInternalProperties()
        .put(AvailableAppSettings.USER_PASSWORD_LENGTH_MIN, "8");
    assertNotNull(policy.validate(null, "abc"));
  }

  @Test
  void defaultMinLengthIs8() {
    assertNull(policy.validate(null, "abcdefgh"));
    assertNotNull(policy.validate(null, "abcdefg"));
  }

  @Test
  void passwordPaddedWithSpacesIsInvalid() {
    assertNotNull(policy.validate(null, "             1"));
  }

  @Test
  void zeroOrNegativeMinLengthFallsBackTo8() {
    AppSettings.get()
        .getInternalProperties()
        .put(AvailableAppSettings.USER_PASSWORD_LENGTH_MIN, "0");
    assertNotNull(policy.validate(null, "short"));
    assertNull(policy.validate(null, "abcdefgh"));
  }

  @Test
  void minLengthBelowHardMinimumIsSilentlyEnforcedTo4() {
    AppSettings.get()
        .getInternalProperties()
        .put(AvailableAppSettings.USER_PASSWORD_LENGTH_MIN, "2");
    // configured value of 2 is below the hard minimum of 4 — policy enforces 4
    assertNull(policy.validate(null, "abcd"));
    assertNotNull(policy.validate(null, "abc"));
  }
}
