/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
package com.axelor.app.internal;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.logging.LoggerConfiguration;
import java.util.Properties;
import java.util.function.Predicate;

/** Helper to install/uninstall application logger. */
public final class AppLogger {

  private static LoggerConfiguration configuration;

  private AppLogger() {}

  private static LoggerConfiguration createLoggerConfig() {
    final AppSettings settings = AppSettings.get();
    final Properties loggingConfig = new Properties();
    final Predicate<String> isLogging = (n) -> n.startsWith("logging.");
    settings.getProperties().stringPropertyNames().stream()
        .filter(isLogging)
        .forEach(
            n -> {
              loggingConfig.setProperty(n, settings.get(n));
            });
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
