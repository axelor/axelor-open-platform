/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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
package com.axelor.shell.commands;

import static com.axelor.common.StringUtils.isBlank;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.gradle.jarjar.com.google.common.collect.Lists;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import com.axelor.common.StringUtils;
import com.axelor.shell.core.CommandProvider;
import com.axelor.shell.core.CommandResult;
import com.axelor.shell.core.Shell;
import com.axelor.shell.core.annotations.CliCommand;
import com.axelor.shell.core.annotations.CliOption;
import com.google.common.base.Joiner;

public class GradleCommands implements CommandProvider {
	
	private GradleConnector connector;
	
	private Shell shell;
	
	public GradleCommands(Shell shell) {
		this.shell = shell;
	}
	
	private File getHome() {
		int count = 5;
		File home = new File(getClass().getProtectionDomain().getCodeSource()
				.getLocation().getFile());
		
		while (count-- > 0 && home != null) {
			File found = new File(home, "init.d/01-plugins.gradle");
			if (found.exists()) {
				return home;
			}
			home = home.getParentFile();
		}
		return null;
	}
	
	private List<File> getInitScripts() {
		File home = getHome();
		if (home == null || !home.exists()) {
			return null;
		}
		final List<File> files = new ArrayList<>();
		for (File file : new File(home, "init.d").listFiles()) {
			if (file.getName().endsWith(".gradle")) {
				files.add(file);
			}
		}
		Collections.sort(files);
		return files;
	}
	
	private CommandResult execute(String... args) {
		
		List<String> arguments = new ArrayList<>();
		List<File> initScripts = getInitScripts();
		
		if (initScripts == null) {
			throw new IllegalStateException("Unable to locate init scripts.");
		}
		
		for (File file : getInitScripts()) {
			arguments.add("-I");
			arguments.add(file.getAbsolutePath());
		}
		for (String arg : args) {
			arguments.add(arg);
		}
		
		if (connector == null) {
			connector = GradleConnector.newConnector();
		}
		
		connector.forProjectDirectory(shell.getWorkingDir());
		ProjectConnection connection = connector.connect();
		
		try {
			BuildLauncher launcher = connection.newBuild();
			launcher.withArguments(arguments.toArray(new String[] {}));
			// Run the build
			launcher.run();
		} finally {
			connection.close();
		}
		
		return new CommandResult(true);
	}

	@CliCommand(name = "clean", help = "clean the project build")
	public void clean() {
		execute("-q", "clean");
	}
	
	@CliCommand(name = "build", usage = "[OPTIONS]", help = "build the project")
	public CommandResult build(
			@CliOption(name = "verbose", shortName = 'v', help = "show verbose output")
			boolean verbose) {
		if (verbose) {
			return execute("-x", "test", "--stacktrace", "build");
		}
		return execute("-q", "-x", "test", "build");
	}
	
	@CliCommand(name = "run", usage = "[OPTIONS]", help = "run the embedded tomcat server")
	public CommandResult run(
			@CliOption(name = "port", shortName = 'p', argName = "PORT", help = "alternative port, default is 8080")
			String port,
			@CliOption(name = "config", shortName = 'c', argName = "FILE", help = "application configuration file")
			String config,
			@CliOption(name = "debug", shortName = 'd', help = "run in debug mode.", defaultValue = "true")
			boolean debug,
			@CliOption(name = "war", shortName = 'w', help = "run the war package")
			boolean war) {
		
		final List<String> args = Lists.newArrayList("-q", "-x", "test");
		if (!isBlank(config)) {
			args.add("-Daxelor.config=" + config);
		}
		if (!isBlank(port)) {
			args.add("-Phttp.port=" + port);
		}
		if (war) {
			args.add("tomcatRunWar");
		} else {
			args.add("tomcatRun");
		}
		return execute(args.toArray(new String[] {}));
	}

	@CliCommand(name = "i18n", usage = "[OPTIONS]", help = "extract/update translatable messages")
	public CommandResult i18n(
			@CliOption(name = "project-dir", shortName = 'p', help = "specify the module directory")
			String module,
			@CliOption(name = "extract", shortName = 'e', help = "extract messages.")
			boolean extract,
			@CliOption(name = "update", shortName = 'u', help = "update messages.")
			boolean update) {
		if (StringUtils.isBlank(module)) {
			return execute("-q", "-x", "test", "i18n-extract");
		}
		return execute("-q", "-x", "test", "-p", module, "i18n-extract");
	}

	@CliCommand(name = "init", usage = "[OPTIONS] [MODULES...]", help = "initialize or update the database")
	public CommandResult init(
			@CliOption(name = "config", shortName = 'c', argName = "FILE", help = "application configuration file", required = true)
			String config,
			@CliOption(name = "update", shortName = 'u', help = "update the installed or given modules")
			boolean update,
			String... modules) {

		final List<String> args = Lists.newArrayList("-q", "-x", "test", "init", "-Daxelor.config=" + config);
		if (update) {
			args.add("-Pupdate=true");
		}
		if (modules != null && modules.length > 0) {
			args.add("-Pmodules=" + Joiner.on(",").join(modules));
		}
		return execute(args.toArray(new String[] {}));
	}
}
