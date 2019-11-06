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

import com.axelor.tomcat.TomcatRunner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AppRunner {

  private static Logger log = LoggerFactory.getLogger(AppRunner.class);

  public static void main(String[] args) {
    AppLogger.install();
    try {
      run(args);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      AppLogger.uninstall();
    }
  }

  private static void run(String[] args) {
    Path baseWebapp = Paths.get("build", "webapp");
    Path configFile = Paths.get("build", "tomcat", "axelor-tomcat.properties");

    if (Files.notExists(baseWebapp)) {
      log.error("Please run './gradlew copyWebapp' and try again...");
      return;
    }

    if (Files.notExists(configFile)) {
      log.error("Plese run './gradlew runnerConfig' and try again...");
      return;
    }

    TomcatRunner.main(args);
  }
}
