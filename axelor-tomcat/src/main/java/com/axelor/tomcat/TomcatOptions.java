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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class TomcatOptions {

	private static final String OPTION_HELP = "help";
	private static final String OPTION_PORT = "port";
	private static final String OPTION_BASE = "base-dir";
	private static final String OPTION_CONTEXT = "context-path";
	private static final String OPTION_CLASSES = "extra-classes";
	private static final String OPTION_LIBS = "extra-libs";

	private int port = 8080;

	private Path baseDir;
	
	private String contextPath = "/";

	private List<Path> roots;

	private List<Path> classes;

	private List<Path> libs;
	
	private boolean help;

	final Options options = new Options();

	public TomcatOptions() {
		addOption(OPTION_HELP, null, "Show this help");
		addOption(OPTION_BASE, "DIR", "The tomcat server base directory.");
		addOption(OPTION_PORT, "NUMBER", "The tomcat server port.");
		addOption(OPTION_CONTEXT, "PATH", "The context path to use for the app.");
		addOption(OPTION_CLASSES, "DIR,...", "The list of extra classes dirs.");
		addOption(OPTION_LIBS, "JAR,...", "The list of extra jar libs.");
	}

	private Option addOption(String name, String argName, String desc) {
		final Option option = Option.builder().longOpt(name).desc(desc).build();
		if (argName != null) {
			option.setArgName(argName);
			option.setArgs(1);
		}
		options.addOption(option);
		return option;
	}
	
	public void parse(String[] args) throws ParseException {
		final CommandLineParser parser = new DefaultParser();
		final CommandLine cli = parser.parse(options, args);
		help = cli.hasOption(OPTION_HELP);
		if (help) {
			return;
		}

		final List<String> paths = cli.getArgList();
		if (paths == null || paths.isEmpty()) {
			throw new ParseException("at-least one webapp dir is required.");
		}

		roots = paths.stream().map(Paths::get).collect(Collectors.toList());
		if (roots.isEmpty()) {
			throw new ParseException("at-least one webapp dir is required.");
		}

		contextPath = cli.getOptionValue(OPTION_CONTEXT, "/");
		if (contextPath.indexOf('/') != 0) {
			contextPath = "/" + contextPath;
		}

		try {
			port = Integer.parseInt(cli.getOptionValue(OPTION_PORT, "" + port));
		} catch (Exception e) {
			throw new ParseException("invalid port value.");
		}

		if (cli.hasOption(OPTION_BASE)) {
			baseDir = Paths.get(cli.getOptionValue(OPTION_BASE));
			if (Files.exists(baseDir) && Files.isRegularFile(baseDir)) {
				throw new ParseException("invalid base directory.");
			}
		}
		if (baseDir == null) {
			baseDir = Paths.get("build/tomcat");
		}

		classes = Stream.of(cli.getOptionValue(OPTION_CLASSES, "").split(","))
				.map(String::trim)
				.map(Paths::get)
				.collect(Collectors.toList());

		libs = Stream.of(cli.getOptionValue(OPTION_LIBS, "").split(","))
				.map(String::trim)
				.map(Paths::get)
				.collect(Collectors.toList());
	}

	public boolean hasHelp() {
		return help;
	}

	public void usage() {
		final HelpFormatter formatter = new HelpFormatter();
		formatter.setOptionComparator(null);
		formatter.printHelp("tomcat-runner [options] <WEBAPP>", options);
	}

	public Path getBaseDir() {
		return baseDir;
	}

	public int getPort() {
		return port;
	}

	public String getContextPath() {
		return contextPath;
	}
	
	public Path getDocBase() {
		if (roots == null) {
			return Paths.get("src/main/webapp");
		}
		return roots.get(0);
	}

	public List<Path> getExtraResources() {
		if (roots == null || roots.isEmpty()) {
			return null;
		}
		return roots.stream().skip(1).collect(Collectors.toList());
	}

	public List<Path> getRoots() {
		return roots;
	}
	
	public List<Path> getClasses() {
		return classes;
	}
	
	public List<Path> getLibs() {
		return libs;
	}
}
