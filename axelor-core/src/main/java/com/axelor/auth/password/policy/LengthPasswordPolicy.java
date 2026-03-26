/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.password.policy;

import com.axelor.auth.password.PasswordPolicy;
import com.google.inject.ImplementedBy;

/** Defines a password policy based on the required minimum password length. */
@ImplementedBy(DefaultLengthPasswordPolicy.class)
public interface LengthPasswordPolicy extends PasswordPolicy {

  String ID = "length";

  @Override
  default String getPolicyId() {
    return ID;
  }

  @Override
  default boolean isMandatory() {
    return true;
  }
}
