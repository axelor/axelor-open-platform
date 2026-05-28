/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.password;

import com.axelor.auth.db.User;
import com.axelor.auth.password.policy.InvalidPolicy;
import com.axelor.auth.password.policy.PolicyDescription;
import com.google.inject.ImplementedBy;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * Manages password validation by aggregating all registered {@link PasswordPolicy} rules.
 *
 * <p>Policies are contributed via Guice Multibindings, allowing downstream modules to add custom
 * rules without replacing the manager.
 */
@ImplementedBy(DefaultAuthPasswordManager.class)
public interface AuthPasswordManager {

  /**
   * Validates the given password against all registered policies.
   *
   * <p>Policies are checked in sequence and stop at the first violation.
   *
   * @param password the plain-text password to validate
   * @param user the user for context-aware checks, or {@code null} for new users
   * @return an {@link InvalidPolicy} describing the first violated policy, or {@code null} if all
   *     policies pass
   */
  InvalidPolicy validate(String password, @Nullable User user);

  /**
   * Returns the descriptions of all currently enabled policies, in evaluation order. Policies that
   * return {@code null} from {@link PasswordPolicy#getDescription()} are excluded.
   *
   * <p>The resulting list is intended to be displayed as guidance on the login or change-password
   * page, before the user submits a new password.
   *
   * @return an ordered list of policy descriptions
   */
  List<PolicyDescription> getDescriptions();
}
