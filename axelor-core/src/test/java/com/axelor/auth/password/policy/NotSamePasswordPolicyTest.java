/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.password.policy;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.axelor.JpaTest;
import com.axelor.auth.AuthService;
import com.axelor.auth.db.User;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

public class NotSamePasswordPolicyTest extends JpaTest {

  @Inject private AuthService authService;

  private final NotSamePasswordPolicy policy = new DefaultNotSamePasswordPolicy();

  @Test
  void validationPassesWhenUserIsNull() {
    assertNull(policy.validate(null, "anypassword"));
  }

  @Test
  void validationPassesWhenUserHasNoPassword() {
    User user = new User("john", "John Doe");
    assertNull(policy.validate(user, "anypassword"));
  }

  @Test
  void validationPassesWhenUserPasswordIsBlank() {
    User user = new User("john", "John Doe");
    user.setPassword("");
    assertNull(policy.validate(user, "anypassword"));
  }

  @Test
  void validationFailsWhenPasswordIsSameAsCurrentPassword() {
    User user = new User("john", "John Doe");
    user.setPassword(authService.encrypt("currentPassword1"));
    assertNotNull(policy.validate(user, "currentPassword1"));
  }

  @Test
  void validationPassesWhenPasswordDiffersFromCurrentPassword() {
    User user = new User("john", "John Doe");
    user.setPassword(authService.encrypt("currentPassword1"));
    assertNull(policy.validate(user, "newPassword2"));
  }
}
