/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script.policy;

public class AllowedByConfiguration {

  public static String myStaticValue = "AllowedByConfiguration";

  public static String myStaticMethod() {
    return "AllowedByConfiguration";
  }

  public static class InnerAllowed {
    public static String myStaticMethod() {
      return "InnerAllowedByConfiguration";
    }
  }
}
