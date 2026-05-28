/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth;

import com.axelor.auth.db.User;
import com.axelor.script.ScriptAllowed;

/** User service */
@ScriptAllowed
public class UserService {

  /**
   * Checks whether the password matches the configured pattern.
   *
   * @param password the password to validate
   * @return true if the password matches the pattern, false otherwise
   */
  public boolean validatePassword(String password) {
    User user = AuthUtils.getUser();
    if (user == null) {
      return false;
    }
    return AuthService.getInstance().validatePasswordPolicies(user, password) == null;
  }
}
