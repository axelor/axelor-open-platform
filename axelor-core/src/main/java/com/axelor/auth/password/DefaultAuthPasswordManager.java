/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.password;

import com.axelor.app.AppSettings;
import com.axelor.auth.db.User;
import com.axelor.auth.password.policy.InvalidPolicy;
import com.axelor.auth.password.policy.PolicyDescription;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Default implementation of {@link AuthPasswordManager}. */
@Singleton
public class DefaultAuthPasswordManager implements AuthPasswordManager {

  private final Set<PasswordPolicy> policies;

  @Inject
  public DefaultAuthPasswordManager(Set<PasswordPolicy> policies) {
    this.policies = policies;
  }

  @Override
  public InvalidPolicy validate(String password, @Nullable User user) {
    for (final var policy : policies) {
      if (!isEnabled(policy)) {
        continue;
      }
      InvalidPolicy invalidPolicy = policy.validate(user, password);
      if (invalidPolicy != null) {
        return invalidPolicy;
      }
    }
    return null;
  }

  @Override
  public List<PolicyDescription> getDescriptions() {
    return policies.stream()
        .filter(this::isEnabled)
        .map(PasswordPolicy::getDescription)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private boolean isEnabled(PasswordPolicy policy) {
    if (policy.isMandatory()) return true;
    String key = "user.password." + policy.getPolicyId() + ".enabled";
    return AppSettings.get().getBoolean(key, policy.isEnabledByDefault());
  }
}
