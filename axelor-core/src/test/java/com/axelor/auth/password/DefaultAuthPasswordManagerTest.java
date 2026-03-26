/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.password;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.axelor.TestingHelpers;
import com.axelor.app.AppSettings;
import com.axelor.auth.db.User;
import com.axelor.auth.password.policy.DefaultDigitsPasswordPolicy;
import com.axelor.auth.password.policy.DefaultLengthPasswordPolicy;
import com.axelor.auth.password.policy.DefaultNotCodePasswordPolicy;
import com.axelor.auth.password.policy.DigitsPasswordPolicy;
import com.axelor.auth.password.policy.LengthPasswordPolicy;
import com.axelor.auth.password.policy.NotCodePasswordPolicy;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultAuthPasswordManagerTest {

  @BeforeEach
  void setUp() {
    TestingHelpers.resetSettings();
  }

  @AfterEach
  void tearDown() {
    TestingHelpers.resetSettings();
  }

  @Test
  void mandatoryPolicyIsAlwaysApplied() {
    // LengthPasswordPolicy is mandatory — always enforced regardless of config
    var manager = new DefaultAuthPasswordManager(Set.of(new DefaultLengthPasswordPolicy()));
    assertNotNull(manager.validate("short", null));
  }

  @Test
  void mandatoryPolicyCannotBeDisabledViaConfig() {
    AppSettings.get()
        .getInternalProperties()
        .put("user.password." + LengthPasswordPolicy.ID + ".enabled", "false");
    var manager = new DefaultAuthPasswordManager(Set.of(new DefaultLengthPasswordPolicy()));
    // setting enabled=false has no effect on a mandatory policy
    assertNotNull(manager.validate("short", null));
  }

  @Test
  void enabledByDefaultPolicyIsApplied() {
    // NotCodePasswordPolicy is enabled by default but not mandatory
    var user = new User();
    user.setCode("admin");
    var manager = new DefaultAuthPasswordManager(Set.of(new DefaultNotCodePasswordPolicy()));
    assertNotNull(manager.validate("myadminpassword", user));
  }

  @Test
  void enabledByDefaultPolicyCanBeDisabled() {
    AppSettings.get()
        .getInternalProperties()
        .put("user.password." + NotCodePasswordPolicy.ID + ".enabled", "false");
    var user = new User();
    user.setCode("admin");
    var manager = new DefaultAuthPasswordManager(Set.of(new DefaultNotCodePasswordPolicy()));
    assertNull(manager.validate("myadminpassword", user));
  }

  @Test
  void disabledByDefaultPolicyIsSkipped() {
    // DigitsPasswordPolicy is disabled by default
    var manager = new DefaultAuthPasswordManager(Set.of(new DefaultDigitsPasswordPolicy()));
    assertNull(manager.validate("nodigitshere", null));
  }

  @Test
  void disabledByDefaultPolicyCanBeEnabled() {
    AppSettings.get()
        .getInternalProperties()
        .put("user.password." + DigitsPasswordPolicy.ID + ".enabled", "true");
    var manager = new DefaultAuthPasswordManager(Set.of(new DefaultDigitsPasswordPolicy()));
    assertNotNull(manager.validate("nodigitshere", null));
  }
}
