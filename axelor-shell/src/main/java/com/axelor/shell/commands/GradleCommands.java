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
package com.axelor.shell.commands;

import static com.axelor.common.StringUtils.isBlank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.axelor.shell.core.CommandProvider;
import com.axelor.shell.core.CommandResult;
import com.axelor.shell.core.Shell;
import com.axelor.shell.core.annotations.CliCommand;
import com.axelor.shell.core.annotations.CliOption;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class GradleCommands implements CommandProvider {

	private Shell shell;

	public GradleCommands(Shell shell) {
		this.shell = shell;
	}

	private CommandResult execute(String... args) {
		return execute(Arrays.asList(args));
	}

	private CommandResult execute(List<String> args) {
		final List<String> params = new ArrayList<>();
		final boolean isWindow = System.getProperty("os.name").toLowerCase().indexOf("windows") > -1;

		params.add(isWindow ? "gradlew.bat" : "./gradlew");
		params.addAll(args);
		
		final ProcessBuilder pb = new ProcessBuilder(params);
		pb.directory(shell.getWorkingDir());
		pb.inheritIO();
		try {
			final Process proc = pb.start();
			try {
				proc.waitFor();
			} catch (InterruptedException e) {
				// ignore interruption error
			}
			return new CommandResult(true);
		} catch (IOException e) {
			return new CommandResult(false);
		}
	}

	@CliCommand(name = "clean", help = "clean the project build")
	public void clean() {
		execute("clean");
	}

	@CliCommand(name = "build", usage = "[OPTIONS]", help = "build the project")
	public CommandResult build(
			@CliOption(name = "quiet", shortName = 'q', help = "show errors only")
			boolean quiet,
			@CliOption(name = "stacktrace", shortName = 's', help = "show stacktrace for all exceptions")
			boolean stacktrace) {
		final List<String> args = Lists.newArrayList("-x", "test");
		if (quiet) { args.add("-q"); }
		if (stacktrace) { args.add("--stacktrace"); }
		args.add("build");
		return execute(args);
	}
	
	@CliCommand(name = "run", usage = "[OPTIONS]", help = "run the embedded tomcat server")
	public CommandResult run(
			@CliOption(name = "port", shortName = 'p', argName = "PORT", help = "alternative port, default is 8080")
			String port,
			@CliOption(name = "config", shortName = 'c', argName = "FILE", help = "application configuration file")
			String config,
			@CliOption(name = "verbose", shortName = 'v', help = "verbose output")
			boolean verbose) {
		
		final List<String> args = Lists.newArrayList("-x", "test");
		if (!verbose) {
			args.add("-q");
			shell.info("Starting Tomcat Server...");
		}
		if (!isBlank(config)) {
			args.add("-Daxelor.config=" + config);
		}
		if (!isBlank(port)) {
			args.add("-Phttp.port=" + port);
		}
		args.add("tomcatRun");
		args.add("--no-daemon");
		return execute(args);
	}

	@CliCommand(name = "i18n", usage = "[OPTIONS]", help = "extract/update translatable messages")
	public CommandResult i18n(
			@CliOption(name = "project-dir", shortName = 'p', help = "specify the module directory")
			String module,
			@CliOption(name = "extract", shortName = 'e', help = "extract messages.")
			boolean extract,
			@CliOption(name = "update", shortName = 'u', help = "update messages.")
			boolean update,
			@CliOption(name = "with-context", shortName = 'c', help = "extract context details.")
			boolean withContext) {
		final String task = update ? "i18n-update" : "i18n-extract";
		final List<String> args = Lists.newArrayList("-x", "test");
		if (withContext) {
			args.add("-Pwith.context=true");
		}
		if (!isBlank(module)) {
			args.add("-p");
			args.add(module);
		}
		args.add(task);
		return execute(args);
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
		return execute(args);
	}

	@CliCommand(name = "migrate", usage = "[OPTIONS]", help = "run database migration scripts")
	public CommandResult migrate(
			@CliOption(name = "config", shortName = 'c', argName = "FILE", help = "application configuration file", required = true)
			String config,
			@CliOption(name = "verbose", shortName = 'v', help = "verbose output")
			boolean verbose) {
		final List<String> args = Lists.newArrayList("-q", "-x", "test", "migrate", "-Daxelor.config=" + config);
		if (verbose) {
			args.add("-Pverbose=true");
		}
		return execute(args);
	}
}
