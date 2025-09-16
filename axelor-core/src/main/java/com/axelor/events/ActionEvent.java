/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.events;

import com.axelor.rpc.Context;

public abstract class ActionEvent {

  private final String name;

  private final Context context;

  public ActionEvent(String name, Context context) {
    this.name = name;
    this.context = context;
  }

  public String getName() {
    return name;
  }

  public Context getContext() {
    return context;
  }
}
