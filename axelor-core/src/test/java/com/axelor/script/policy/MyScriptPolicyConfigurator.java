/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script.policy;

import com.axelor.script.ScriptPolicyConfigurator;
import java.util.List;

public class MyScriptPolicyConfigurator implements ScriptPolicyConfigurator {

  @Override
  public void configure(
      List<String> allowPackages,
      List<Class<?>> allowClasses,
      List<String> denyPackages,
      List<Class<?>> denyClasses) {

    allowClasses.add(AllowedByConfiguration.class);
    allowClasses.add(AllowedByConfiguration.InnerAllowed.class);
    allowClasses.add(MyOtherService.class);

    denyClasses.add(DeniedByConfiguration.class);
  }
}
