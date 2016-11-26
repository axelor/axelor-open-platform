/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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
package com.axelor.common.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.common.FileUtils;

public class LoggingTest {

	@Test
	public void test() throws Exception {
		final Properties properties = new Properties();
		final Path logPath = Files.createTempDirectory("axelor");

		properties.setProperty("logging.path", logPath.toString());
		properties.setProperty("logging.level.com.axelor", "trace");

		final LoggerConfiguration loggerConfig = new LoggerConfiguration(properties);

		final PrintStream sout = System.out;
		final StringBuilder builder = new StringBuilder();
		final OutputStream out = new OutputStream() {

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

			Assert.assertTrue(output.contains("Test info..."));
			Assert.assertTrue(output.contains("Test warn..."));
			Assert.assertTrue(output.contains("Test error..."));
			Assert.assertTrue(output.contains("Test trace..."));
			Assert.assertTrue(output.contains("Test JUL..."));
			Assert.assertTrue(logPath.resolve("axelor.log").toFile().exists());
		} finally {
			System.setOut(sout);
			loggerConfig.uninstall();
			FileUtils.deleteDirectory(logPath);
		}
	}
}
