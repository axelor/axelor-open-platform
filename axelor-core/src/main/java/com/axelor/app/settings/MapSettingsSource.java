/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.app.settings;

import java.util.Map;

public class MapSettingsSource extends AbstractSettingsSource {

  public MapSettingsSource(Map<String, String> map) {
    super(map);
  }
}
