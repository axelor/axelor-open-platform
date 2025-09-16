/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.app;

import com.axelor.event.Observes;
import com.axelor.events.StartupEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppSettingsObserver {

  private static final Logger log = LoggerFactory.getLogger(AppSettingsObserver.class);

  void onAppStartup(@Observes StartupEvent event) {
    final AppSettings settings = AppSettings.get();

    for (final String setting :
        List.of(
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
