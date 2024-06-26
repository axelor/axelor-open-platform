/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script.policy;

import jakarta.inject.Inject;

public class MyServiceImpl implements MyService {

  private final MyYetAnotherService myYetAnotherService;

  @Inject
  public MyServiceImpl(MyYetAnotherService myYetAnotherService) {
    this.myYetAnotherService = myYetAnotherService;
  }

  @Override
  public String myMethod() {
    return "Hello";
  }

  @Override
  public String myYetAnotherMethod() {
    return myYetAnotherService.myYetAnotherMethod();
  }

  @Override
  public MyYetAnotherService getMyYetAnotherService() {
    return myYetAnotherService;
  }
}
