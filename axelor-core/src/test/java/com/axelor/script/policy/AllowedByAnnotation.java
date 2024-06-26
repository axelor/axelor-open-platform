/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script.policy;

import com.axelor.script.ScriptAllowed;

@ScriptAllowed
public class AllowedByAnnotation {

  public String myValue = "AllowedByAnnotation";

  public String myMethod() {
    return "AllowedByAnnotation";
  }

  @ScriptAllowed
  public static class InnerAllowed {
    public String myMethod() {
      return "InnerAllowedByAnnotation";
    }
  }

  public static class InnerDenied {
    public String myMethod() {
      return "InnerDeniedByDefault";
    }
  }
}
