/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2020 Axelor (<http://axelor.com>).
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
import java.io.File;
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

  private static boolean prepare() {
    Path configFile = Paths.get("build", "tomcat", "axelor-tomcat.properties");
    Path webapp = Paths.get("build", "webapp");
    if (Files.exists(configFile) && Files.exists(webapp)) {
      return true;
    }

    log.info("Preparing...");

    Path gradlew =
        Paths.get(File.separatorChar == '\\' ? "gradlew.bat" : "gradlew")
            .normalize()
            .toAbsolutePath();

    ProcessBuilder builder = new ProcessBuilder(gradlew.toString(), "runnerConfig");
    try {
      Process process = builder.start();
      if (process.waitFor() != 0 || (Files.notExists(configFile) && Files.notExists(configFile))) {
        log.error("Please run './gradlew runnerConfig' and try again.");
        return false;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }

    return true;
  }

  private static void run(String[] args) {
    if (prepare()) {
      log.info("Starting...");
      TomcatRunner.main(args);
    }
  }
}
