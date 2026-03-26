/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.password.policy;

import com.axelor.auth.password.PasswordPolicy;
import com.google.inject.ImplementedBy;

/** Defines a password policy that prevents reusing the current password. */
@ImplementedBy(DefaultNotSamePasswordPolicy.class)
public interface NotSamePasswordPolicy extends PasswordPolicy {

  String ID = "notSame";

  @Override
  default String getPolicyId() {
    return ID;
  }

  @Override
  default boolean isMandatory() {
    return true;
  }
}
