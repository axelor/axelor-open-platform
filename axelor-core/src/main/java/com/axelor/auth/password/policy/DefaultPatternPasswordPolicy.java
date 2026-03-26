/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.password.policy;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import java.util.regex.Pattern;

/** Default implementation of {@link PatternPasswordPolicy}. */
@Singleton
public class DefaultPatternPasswordPolicy implements PatternPasswordPolicy {

  private static final String ERROR_MESSAGE = /*$$(*/ "must match the pattern" /*)*/;
  private static final String DESCRIPTION = /*$$(*/ "validate the pattern" /*)*/;

  @Override
  public InvalidPolicy validate(@Nullable User user, String password) {
    String value = AppSettings.get().get(AvailableAppSettings.USER_PASSWORD_PATTERN_VALUE, null);
    if (StringUtils.isBlank(value)) {
      return null;
    }

    if (!Pattern.compile(value).matcher(password).matches()) {
      return new InvalidPolicy(ERROR_MESSAGE);
    }
    return null;
  }

  @Override
  public PolicyDescription getDescription() {
    String value = AppSettings.get().get(AvailableAppSettings.USER_PASSWORD_PATTERN_VALUE, null);
    if (StringUtils.isBlank(value)) {
      return null;
    }
    return new PolicyDescription(ID, DESCRIPTION);
  }
}
