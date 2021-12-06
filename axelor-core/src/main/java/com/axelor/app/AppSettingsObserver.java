/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
            AvailableAppSettings.APPLICATION_DISABLE_PERMISSION_RELATIONAL_FIELDS,
            AvailableAppSettings.APPLICATION_DISABLE_PERMISSION_ACTIONS)) {
      if (settings.getBoolean(setting, false)) {
        log.warn(
            "\"{}\" breaks security. Use with caution and as last resort only. Will be removed in the future.",
            setting);
      }
    }
  }
}
