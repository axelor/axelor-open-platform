/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptPolicyConfiguratorService {

  private final Set<ScriptPolicyConfigurator> configurators;

  private static final Logger log = LoggerFactory.getLogger(ScriptPolicyConfiguratorService.class);

  @Inject
  public ScriptPolicyConfiguratorService(Set<ScriptPolicyConfigurator> configurators) {
    this.configurators = configurators;
  }

  public void configure(
      List<String> allowPackages,
      List<Class<?>> allowClasses,
      List<String> denyPackages,
      List<Class<?>> denyClasses) {
    for (var configurator : configurators) {
      try {
        configurator.configure(allowPackages, allowClasses, denyPackages, denyClasses);
      } catch (Exception e) {
        log.error(
            "Failed to configure script policy with configurator {}",
            configurator.getClass().getName(),
            e);
      }
    }
  }
}
