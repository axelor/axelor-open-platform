/**
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

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class TomcatOptions {

	private int port = 8080;

	private File baseDir;

	private File webappDir;

	private String contextPath;
	
	private boolean help;

	final Options options = new Options();

	public TomcatOptions() {

		final Option help = Option.builder("h")
				.longOpt("help")
				.desc("show help")
				.build();

		final Option port = Option.builder("p")
				.longOpt("port")
				.argName("NUMBER")
				.hasArg()
				.desc("tomcat server port.")
				.build();

		final Option base = Option.builder()
				.longOpt("base-dir")
				.argName("DIR")
				.hasArg()
				.desc("tomcat base directory.")
				.build();
		
		final Option path = Option.builder()
				.longOpt("context-path")
				.argName("PATH")
				.hasArg()
				.desc("the webapp context path")
				.build();
		
		options.addOption(help);
		options.addOption(port);
		options.addOption(base);
		options.addOption(path);
	}
	
	public void parse(String[] args) throws Exception {
		final CommandLineParser parser = new DefaultParser();
		final CommandLine cli = parser.parse(options, args);
		help = cli.hasOption("h");
		if (help) {
			return;
		}

		final List<String> paths = cli.getArgList();
		if (paths == null || paths.isEmpty()) {
			throw new ParseException("webapp is require.");
		}
		webappDir = Paths.get(paths.get(0)).toFile();
		if (!webappDir.exists()) {
			throw new ParseException(String.format("'%s' doesn't exist", webappDir));
		}
		if (!webappDir.isDirectory()) {
			throw new ParseException(String.format("'%s' is not expanded webapp", webappDir));
		}

		contextPath = cli.getOptionValue("context-path", "");
		if (cli.hasOption("p")) {
			port = Integer.parseInt(cli.getOptionValue("p"));
		}
		if (cli.hasOption("base-dir")) {
			baseDir = Paths.get(cli.getOptionValue("base-dir")).toAbsolutePath().toFile();
		}
	}

	public File getBaseDir() {
		if (baseDir == null) {
			baseDir = Paths.get("build", "tomcat").toFile();
		}
		return baseDir;
	}

	public int getPort() {
		return port;
	}

	public File getWebappDir() {
		return webappDir;
	}

	public String getContextPath() {
		if (contextPath == null) {
			contextPath = "/";
		}
		if (!contextPath.startsWith("/")) {
			contextPath = "/" + contextPath;
		}
		return contextPath;
	}

	public boolean hasHelp() {
		return help;
	}

	public void usage() {
		final HelpFormatter formatter = new HelpFormatter();
		formatter.setOptionComparator(null);
		formatter.printHelp("tomcat-runner [options] <WEBAPP>", options);
	}
}
