/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.password.policy;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.axelor.auth.db.User;
import org.junit.jupiter.api.Test;

public class NotCodePasswordPolicyTest {

  private final NotCodePasswordPolicy policy = new DefaultNotCodePasswordPolicy();

  @Test
  void validationPassesWhenUserIsNull() {
    assertNull(policy.validate(null, "anypassword"));
  }

  @Test
  void validationPassesWhenPasswordDoesNotContainCode() {
    User user = new User("john", "John Doe");
    assertNull(policy.validate(user, "c0rr3ct-h0rs3!"));
  }

  @Test
  void validationFailsWhenPasswordEqualsCode() {
    User user = new User("john", "John Doe");
    assertNotNull(policy.validate(user, "john"));
  }

  @Test
  void validationFailsWhenPasswordContainsCode() {
    User user = new User("john", "John Doe");
    assertNotNull(policy.validate(user, "john123!"));
    assertNotNull(policy.validate(user, "super_john_pass!"));
  }

  @Test
  void containsCheckIsCaseInsensitive() {
    User user = new User("john", "John Doe");
    assertNotNull(policy.validate(user, "JOHN123!"));
    assertNotNull(policy.validate(user, "myJohnPass1!"));
  }

  @Test
  void validationPassesWhenCodeIsBlank() {
    User user = new User();
    user.setCode("");
    assertNull(policy.validate(user, "anypassword"));
  }
}
