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
package com.axelor.common.logging;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.common.FileUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingTest {

  @Test
  public void test() throws Exception {
    final Properties properties = new Properties();
    final Path logPath = Files.createTempDirectory("axelor");

    properties.setProperty("logging.path", logPath.toString());
    properties.setProperty("logging.level.com.axelor", "trace");

    final LoggerConfiguration loggerConfig = new LoggerConfiguration(properties);
    loggerConfig.skipDefaultConfig(true);

    final PrintStream sout = System.out;
    final StringBuilder builder = new StringBuilder();
    final OutputStream out =
        new OutputStream() {

          @Override
          public void write(int b) throws IOException {
            builder.append((char) b);
          }
        };

    try {
      loggerConfig.install();
      System.setOut(new PrintStream(out));

      final Logger log = LoggerFactory.getLogger(getClass());
      final java.util.logging.Logger jul = java.util.logging.Logger.getLogger(getClass().getName());

      log.info("Test info....");
      log.warn("Test warn....");
      log.error("Test error....");
      log.trace("Test trace....");

      jul.info("Test JUL...");

      final String output = builder.toString();

      assertTrue(output.contains("Test info..."));
      assertTrue(output.contains("Test warn..."));
      assertTrue(output.contains("Test error..."));
      assertTrue(output.contains("Test trace..."));
      assertTrue(output.contains("Test JUL..."));
      assertTrue(logPath.resolve("axelor.log").toFile().exists());
    } finally {
      System.setOut(sout);
      out.close();
      loggerConfig.uninstall();
      FileUtils.deleteDirectory(logPath);
    }
  }
}
