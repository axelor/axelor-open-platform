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

public class ScorePasswordPolicyTest {

  private final ScorePasswordPolicy policy = new DefaultScorePasswordPolicy();

  @BeforeEach
  void setUp() {
    TestingHelpers.resetSettings();
  }

  @AfterEach
  void tearDown() {
    TestingHelpers.resetSettings();
  }

  @Test
  void weakPasswordFailsDefaultMinScore() {
    // "password" scores 0 with zxcvbn, default min is 2
    assertNotNull(policy.validate(null, "password"));
  }

  @Test
  void strongPasswordPassesDefaultMinScore() {
    assertNull(policy.validate(null, "c0rr3ct-h0rs3-b@tt3ry-st4pl3!"));
  }

  @Test
  void weakPasswordPassesWhenMinScoreIsOne() {
    AppSettings.get()
        .getInternalProperties()
        .put(AvailableAppSettings.USER_PASSWORD_SCORE_MIN, "1");
    assertNull(policy.validate(null, "hello123!A"));
  }

  @Test
  void weakPasswordFailsWhenMinScoreIsThree() {
    AppSettings.get()
        .getInternalProperties()
        .put(AvailableAppSettings.USER_PASSWORD_SCORE_MIN, "3");
    assertNotNull(policy.validate(null, "password"));
  }
}
