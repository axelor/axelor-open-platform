/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
