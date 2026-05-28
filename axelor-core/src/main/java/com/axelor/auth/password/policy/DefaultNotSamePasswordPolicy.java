/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.password.policy;

import com.axelor.auth.AuthService;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.inject.Beans;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

/** Default implementation of {@link NotSamePasswordPolicy}. */
@Singleton
public class DefaultNotSamePasswordPolicy implements NotSamePasswordPolicy {

  private static final String ERROR_MESSAGE = /*$$(*/
      "must be different from the current password" /*)*/;
  private static final String DESCRIPTION = /*$$(*/ "be different from the current password" /*)*/;

  @Override
  public InvalidPolicy validate(@Nullable User user, String password) {
    if (user != null
        && StringUtils.notBlank(user.getPassword())
        && Beans.get(AuthService.class).match(password, user.getPassword())) {
      return new InvalidPolicy(ERROR_MESSAGE);
    }
    return null;
  }

  @Override
  public PolicyDescription getDescription() {
    return new PolicyDescription(ID, DESCRIPTION);
  }
}
