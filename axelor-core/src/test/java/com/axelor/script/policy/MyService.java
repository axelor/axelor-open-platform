/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script.policy;

import com.axelor.script.ScriptAllowed;
import java.util.List;

@ScriptAllowed
public interface MyService {
  String MY_CONSTANT = "myConstant";
  List<String> MY_CONSTANT_LIST = List.of("hello", "world");

  String myMethod();

  String myYetAnotherMethod();

  MyYetAnotherService getMyYetAnotherService();
}
