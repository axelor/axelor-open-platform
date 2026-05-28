/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script.policy;

public class SubAllowedByAnnotation extends AllowedByAnnotation {

  @Override
  public String myMethod() {
    return "SubAllowedByAnnotation";
  }
}
