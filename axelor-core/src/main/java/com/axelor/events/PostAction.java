/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.events;

import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;

public class PostAction extends ActionEvent {

  private Object result;

  public PostAction(String name, Context context, Object result) {
    super(name, context);
    this.result = result;
  }

  public Object getResult() {
    return result;
  }

  public void setResult(Object result) {
    if (this.result instanceof ActionResponse
        && result != null
        && !(result instanceof ActionResponse)) {
      String msg = "Expected action result of type '%s' but was '%s'";
      throw new IllegalArgumentException(
          msg.formatted(ActionResponse.class.getName(), result.getClass().getName()));
    }

    this.result = result;
  }
}
