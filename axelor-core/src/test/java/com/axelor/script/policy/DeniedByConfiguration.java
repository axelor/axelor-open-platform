/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script.policy;

import com.axelor.script.ScriptAllowed;

@ScriptAllowed
public class DeniedByConfiguration {

  public static String myStaticValue = "DeniedByConfigurationStatic";
  public String myValue = "DeniedByConfiguration";

  public static String myStaticMethod() {
    return "DeniedByConfiguration";
  }

  @ScriptAllowed
  public static class InnerAllowed {
    public static String myStaticMethod() {
      return "InnerAllowedByAnnotation";
    }
  }

  public static class InnerDenied {
    public static String myStaticMethod() {
      return "InnerDeniedByDefault";
    }
  }
}
