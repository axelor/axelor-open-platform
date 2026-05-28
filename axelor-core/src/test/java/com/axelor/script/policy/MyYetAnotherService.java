/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script.policy;

import java.util.List;

public interface MyYetAnotherService {

  String MY_CONSTANT = "myYetAnotherServiceConstant";
  List<String> MY_CONSTANT_LIST = List.of("goodbye", "world");

  String myYetAnotherMethod();
}
