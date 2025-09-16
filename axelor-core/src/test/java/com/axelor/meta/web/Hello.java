/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.web;

import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class Hello {

  public void say(ActionRequest request, ActionResponse response) {
    response.setInfo("Hello World!!!", "My title");
  }

  @CallMethod
  public String say(String what) {
    return "Say: " + what;
  }

  public String unauthorizedCallMethod(String what) {
    return "Call unauthorizedCallMethod: " + what;
  }
}
