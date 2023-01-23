/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.app;

import com.axelor.event.Observes;
import com.axelor.events.StartupEvent;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppSettingsObserver {

  private static final Logger log = LoggerFactory.getLogger(AppSettingsObserver.class);

  void onAppStartup(@Observes StartupEvent event) {
    final AppSettings settings = AppSettings.get();

    for (final String setting :
        ImmutableList.of(
            AvailableAppSettings.APPLICATION_PERMISSION_DISABLE_RELATIONAL_FIELD,
            AvailableAppSettings.APPLICATION_PERMISSION_DISABLE_ACTION)) {
      if (settings.getBoolean(setting, false)) {
        log.warn(
            "\"{}\" breaks security. Use with caution and as last resort only. Will be removed in the future.",
            setting);
      }
    }
  }
}
