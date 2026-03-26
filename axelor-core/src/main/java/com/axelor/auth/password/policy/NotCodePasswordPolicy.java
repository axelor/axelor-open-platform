/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.password.policy;

import com.axelor.auth.password.PasswordPolicy;
import com.google.inject.ImplementedBy;

/** Defines a password policy that prevents using the user code (login) in the password. */
@ImplementedBy(DefaultNotCodePasswordPolicy.class)
public interface NotCodePasswordPolicy extends PasswordPolicy {

  String ID = "notCode";

  @Override
  default String getPolicyId() {
    return ID;
  }
}
