/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import java.util.List;

public interface ScriptPolicyConfigurator {

  void configure(
      List<String> allowPackages,
      List<Class<?>> allowClasses,
      List<String> denyPackages,
      List<Class<?>> denyClasses);
}
