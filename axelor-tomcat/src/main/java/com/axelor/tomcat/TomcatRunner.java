/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
package com.axelor.tomcat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class TomcatRunner {

	private static final String OPTION_HELP = "help";
	private static final String OPTION_PORT = "port";
	private static final String OPTION_BASE = "base-dir";
	private static final String OPTION_CONTEXT = "context-path";
	private static final String OPTION_CLASSES = "extra-classes";
	private static final String OPTION_LIBS = "extra-libs";

	private static Option addOption(Options options, String name, String argName, String desc) {
		final Option option = Option.builder().longOpt(name).desc(desc).build();
		if (argName != null) {
			option.setArgName(argName);
			option.setArgs(1);
		}
		options.addOption(option);
		return option;
	}

	private static void usage(Options options) {
		final HelpFormatter formatter = new HelpFormatter();
		formatter.setOptionComparator(null);
		formatter.printHelp("tomcat-runner [options] <WEBAPP>", options);
	}

	public static void main(String[] args) {
		final Options options = new Options();
		final CommandLineParser parser = new DefaultParser();
		final CommandLine cli;

		addOption(options, OPTION_HELP, null, "Show this help");
		addOption(options, OPTION_PORT, "NUMBER", "The tomcat server port.");
		addOption(options, OPTION_CONTEXT, "PATH", "The context path to use for the app.");
		addOption(options, OPTION_BASE, "DIR", "The tomcat server base directory.");
		addOption(options, OPTION_CLASSES, "DIR,...", "The list of extra classes dirs.");
		addOption(options, OPTION_LIBS, "JAR,...", "The list of extra jar libs.");

		try {
			cli = parser.parse(options, args);
		} catch (Exception e) {
			usage(options);
			return;
		}

		final List<Path> webapps = cli.getArgList().stream().map(Paths::get).collect(Collectors.toList());

		if (cli.hasOption(OPTION_HELP) || webapps.isEmpty()) {
			usage(options);
			return;
		}

		final TomcatOptions settings = new TomcatOptions(webapps);

		if (cli.hasOption(OPTION_CONTEXT)) {
			settings.setContextPath(cli.getOptionValue(OPTION_CONTEXT));
		}

		if (cli.hasOption(OPTION_PORT)) {
			try {
				settings.setPort(Integer.parseInt(cli.getOptionValue(OPTION_PORT)));
			} catch (NumberFormatException e) {
				System.err.println("Invalid port.");
				return;
			}
		}

		if (cli.hasOption(OPTION_BASE)) {
			Path baseDir = Paths.get(cli.getOptionValue(OPTION_BASE));
			if (Files.exists(baseDir) && Files.isRegularFile(baseDir)) {
				System.err.println("invalid base directory.");
				return;
			}
			settings.setBaseDir(baseDir);
		}
		
		if (cli.hasOption(OPTION_CLASSES)) {
			Arrays.stream(cli.getOptionValues(OPTION_CLASSES))
				.flatMap(value -> Arrays.stream(value.split(",")))
				.map(String::trim)
				.map(Paths::get)
				.forEach(settings::addClasses);
		}
		
		if (cli.hasOption(OPTION_LIBS)) {
			Arrays.stream(cli.getOptionValues(OPTION_LIBS))
				.flatMap(value -> Arrays.stream(value.split(",")))
				.map(String::trim)
				.map(Paths::get)
				.forEach(settings::addLib);
		}

		final TomcatServer server = new TomcatServer(settings);
		server.start();
	}
}
