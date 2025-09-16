/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.events;

import com.axelor.rpc.Context;

public class PreAction extends ActionEvent {

  public PreAction(String name, Context context) {
    super(name, context);
  }
}
