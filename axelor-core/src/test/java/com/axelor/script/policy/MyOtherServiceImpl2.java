/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script.policy;

public class MyOtherServiceImpl2 extends MyOtherServiceImpl {

  @Override
  public int myOtherMethod() {
    return super.myOtherMethod() + 1;
  }
}
