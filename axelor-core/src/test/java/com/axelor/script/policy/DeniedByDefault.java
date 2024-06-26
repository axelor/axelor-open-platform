/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script.policy;

public class DeniedByDefault {

  public static String myStaticMethod() {
    return "DeniedByDefault";
  }

  public static class InnerDenied {
    public static String myStaticMethod() {
      return "InnerDeniedByDefault";
    }
  }
}
