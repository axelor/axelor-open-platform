/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.password.policy;

import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

/** Default implementation of {@link NotCodePasswordPolicy}. */
@Singleton
public class DefaultNotCodePasswordPolicy implements NotCodePasswordPolicy {

  private static final String ERROR_MESSAGE = /*$$(*/ "must not contain the user code" /*)*/;
  private static final String DESCRIPTION = /*$$(*/ "not contain your login code" /*)*/;

  @Override
  public InvalidPolicy validate(@Nullable User user, String password) {
    if (user != null
        && StringUtils.notBlank(user.getCode())
        && password.toLowerCase().contains(user.getCode().toLowerCase())) {
      return new InvalidPolicy(ERROR_MESSAGE);
    }
    return null;
  }

  @Override
  public PolicyDescription getDescription() {
    return new PolicyDescription(ID, DESCRIPTION);
  }
}
