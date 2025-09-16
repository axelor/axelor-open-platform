/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.actions;

public abstract class ActionResumable extends Action {

  private transient int index;

  public int getIndex() {
    return index;
  }

  protected final ActionResumable resumeAt(int index) {
    ActionResumable action = this.copy();
    action.index = index;
    return action;
  }

  protected abstract ActionResumable copy();
}
