/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script.policy;

import jakarta.inject.Inject;

public class MyServiceImpl2 extends MyServiceImpl {

  @Inject
  public MyServiceImpl2(MyYetAnotherService myYetAnotherService) {
    super(myYetAnotherService);
  }

  @Override
  public String myMethod() {
    return super.myMethod() + ", World!";
  }
}
