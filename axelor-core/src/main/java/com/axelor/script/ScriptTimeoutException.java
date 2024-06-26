/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

public class ScriptTimeoutException extends ScriptPolicyException {

  public ScriptTimeoutException() {
    super("Script execution timed out");
  }
}
