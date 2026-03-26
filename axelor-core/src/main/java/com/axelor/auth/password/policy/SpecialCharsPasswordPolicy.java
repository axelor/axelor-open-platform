/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.password.policy;

import com.axelor.auth.password.PasswordPolicy;
import com.google.inject.ImplementedBy;

/** Defines a password policy based on the required minimum number of special characters. */
@ImplementedBy(DefaultSpecialCharsPasswordPolicy.class)
public interface SpecialCharsPasswordPolicy extends PasswordPolicy {

  String ID = "specialChars";

  @Override
  default String getPolicyId() {
    return ID;
  }

  @Override
  default boolean isEnabledByDefault() {
    return false;
  }
}
