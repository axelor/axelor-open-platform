/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.password;

import com.axelor.auth.db.User;
import com.axelor.auth.password.policy.InvalidPolicy;
import com.axelor.auth.password.policy.PolicyDescription;
import jakarta.annotation.Nullable;

/**
 * Represents a single password policy rule.
 *
 * <p>Implementations should return an {@link InvalidPolicy} when the password violates the rule.
 * Multiple policies are aggregated by {@link AuthPasswordManager}.
 */
public interface PasswordPolicy {

  /**
   * Returns the unique identifier of this policy, used to look up its configuration. The
   * corresponding enabled setting is {@code user.password.<policyId>.enabled}.
   *
   * @return the policy identifier
   */
  String getPolicyId();

  /**
   * Returns whether this policy is enabled by default when no explicit configuration is found.
   * Policies that are opt-in should override this to return {@code false}.
   *
   * @return {@code true} if this policy is active by default
   */
  default boolean isEnabledByDefault() {
    return true;
  }

  /**
   * Returns whether this policy is mandatory and cannot be disabled via configuration. Mandatory
   * policies are always enforced regardless of the {@code user.password.<id>.enabled} setting.
   *
   * @return {@code true} if this policy cannot be disabled
   */
  default boolean isMandatory() {
    return false;
  }

  /**
   * Checks whether the given password satisfies this policy rule.
   *
   * @param user the user for context-aware checks, or {@code null} for new users
   * @param password the plain-text password to check
   * @return an {@link InvalidPolicy} if the password violates this rule
   */
  InvalidPolicy validate(@Nullable User user, String password);

  /**
   * Returns a description of this policy's requirement, suitable for display on the login or
   * change-password page as guidance before the user submits. This is distinct from the error
   * message returned on violation.
   *
   * <p>Implementations may return {@code null} when the policy has no meaningful description (e.g.
   * a pattern policy with no pattern configured).
   *
   * @return a {@link PolicyDescription}, or {@code null} if not applicable
   */
  @Nullable
  PolicyDescription getDescription();
}
