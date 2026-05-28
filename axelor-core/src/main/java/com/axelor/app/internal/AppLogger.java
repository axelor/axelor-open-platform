/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.app.internal;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.logging.LoggerConfiguration;
import java.util.Properties;

/** Helper to install/uninstall application logger. */
public final class AppLogger {

  private static LoggerConfiguration configuration;

  private AppLogger() {}

  private static LoggerConfiguration createLoggerConfig() {
    final AppSettings settings = AppSettings.get();
    final Properties loggingConfig = new Properties();
    settings
        .getPropertiesKeysStartingWith("logging.")
        .forEach(n -> loggingConfig.setProperty(n, settings.get(n)));
    if (loggingConfig.containsKey(AvailableAppSettings.LOGGING_PATH)) {
      loggingConfig.setProperty(
          AvailableAppSettings.LOGGING_PATH,
          settings.getPath(AvailableAppSettings.LOGGING_PATH, null));
    }
    return new LoggerConfiguration(loggingConfig);
  }

  public static void install() {
    if (configuration == null) {
      configuration = createLoggerConfig();
      configuration.install();
    }
  }

  public static void uninstall() {
    if (configuration != null) {
      configuration.uninstall();
      configuration = null;
    }
  }
}
