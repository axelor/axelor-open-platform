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

/** Default implementation of {@link LengthPasswordPolicy}. */
@Singleton
public class DefaultLengthPasswordPolicy implements LengthPasswordPolicy {

  // Must be at least, >= min length in database
  private static final int HARD_MIN_LENGTH = 4;

  private static final int DEFAULT_MIN_LENGTH = 8;
  private static final String ERROR_MESSAGE = /*$$(*/ "must be at least minimum length {0}" /*)*/;
  private static final String DESCRIPTION = /*$$(*/ "contain at least {0} characters" /*)*/;

  @Override
  public InvalidPolicy validate(@Nullable User user, String password) {
    int min = getMin();

    if (password.trim().length() < min) {
      return new InvalidPolicy(ERROR_MESSAGE, min);
    }
    return null;
  }

  @Override
  public PolicyDescription getDescription() {
    return new PolicyDescription(ID, DESCRIPTION, getMin());
  }

  protected int getMin() {
    int min =
        AppSettings.get().getInt(AvailableAppSettings.USER_PASSWORD_LENGTH_MIN, DEFAULT_MIN_LENGTH);
    if (min <= 0) {
      min = DEFAULT_MIN_LENGTH;
    } else if (min < HARD_MIN_LENGTH) {
      min = HARD_MIN_LENGTH;
    }
    return min;
  }
}
