/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.password.policy;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.db.User;
import com.nulabinc.zxcvbn.Zxcvbn;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

/**
 * Default implementation of {@link ScorePasswordPolicy}.
 *
 * <p>Uses the <a href="https://github.com/nulab/zxcvbn4j">zxcvbn4j</a> library to estimate password
 * strength. The score ranges from 0 (very weak) to 4 (very strong). The minimum required score is
 * configured via {@code user.password.score.min} (default: 2).
 */
@Singleton
public class DefaultScorePasswordPolicy implements ScorePasswordPolicy {

  private static final String ERROR_MESSAGE = /*$$(*/
      "strength score is too low (minimum required: {0}/4)" /*)*/;
  private static final String DESCRIPTION = /*$$(*/ "be sufficiently strong" /*)*/;

  private static final class ZxcvbnHolder {
    static final Zxcvbn INSTANCE = new Zxcvbn();
  }

  @Override
  public InvalidPolicy validate(@Nullable User user, String password) {
    int min = getMinScore();

    if (ZxcvbnHolder.INSTANCE.measure(password).getScore() < min) {
      return new InvalidPolicy(ERROR_MESSAGE, min);
    }
    return null;
  }

  @Override
  public PolicyDescription getDescription() {
    return new PolicyDescription(ID, DESCRIPTION);
  }

  protected int getMinScore() {
    return AppSettings.get().getInt(AvailableAppSettings.USER_PASSWORD_SCORE_MIN, 2);
  }
}
