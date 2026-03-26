/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.password.policy;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.db.User;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

/** Default implementation of {@link UpperCasePasswordPolicy}. */
@Singleton
public class DefaultUpperCasePasswordPolicy implements UpperCasePasswordPolicy {

  private static final String ERROR_MESSAGE = /*$$(*/
      "must contain at least {0} uppercase character(s)" /*)*/;
  private static final String DESCRIPTION = /*$$(*/
      "contain at least {0} uppercase character(s)" /*)*/;

  @Override
  public InvalidPolicy validate(@Nullable User user, String password) {
    int min = getMin();

    long count = password.chars().filter(Character::isUpperCase).count();
    if (count < min) {
      return new InvalidPolicy(ERROR_MESSAGE, min);
    }
    return null;
  }

  @Override
  public PolicyDescription getDescription() {
    return new PolicyDescription(ID, DESCRIPTION, getMin());
  }

  protected int getMin() {
    return AppSettings.get().getInt(AvailableAppSettings.USER_PASSWORD_UPPER_CASE_MIN, 1);
  }
}
