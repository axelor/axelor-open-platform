/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.app.settings;

import static java.util.stream.Collectors.toMap;

import java.util.Map;

public class EnvSettingSource extends MapSettingsSource {

  public EnvSettingSource() {
    super(getEnvProperties());
  }

  static Map<String, String> getEnvProperties() {
    return parse(System.getenv());
  }

  static Map<String, String> parse(Map<String, String> env) {
    return env.entrySet().stream()
        .filter(e -> e.getKey().startsWith(SettingsUtils.ENV_CONFIG_PREFIX))
        .collect(toMap(EnvSettingSource::processKey, Map.Entry::getValue));
  }

  static String processKey(Map.Entry<String, String> e) {
    return e.getKey()
        .replaceFirst(SettingsUtils.ENV_CONFIG_PREFIX, "")
        .replace('_', '.')
        .replace(' ', '\0')
        .toLowerCase();
  }
}
