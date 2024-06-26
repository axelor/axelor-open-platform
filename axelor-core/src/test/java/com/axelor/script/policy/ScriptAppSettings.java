/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script.policy;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.script.ScriptAllowed;

@ScriptAllowed
public class ScriptAppSettings {

  private static final String DEV = "dev";

  private final AppSettings settings = AppSettings.get();

  public String getApplicationMode() {
    return settings.get(AvailableAppSettings.APPLICATION_MODE, DEV);
  }

  public boolean isProduction() {
    return !DEV.equals(getApplicationMode());
  }
}
