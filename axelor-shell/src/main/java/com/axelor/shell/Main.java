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
package com.axelor.shell;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.axelor.shell.commands.BuiltinCommands;
import com.axelor.shell.commands.GradleCommands;
import com.axelor.shell.commands.ProjectCommands;
import com.axelor.shell.core.Shell;
import com.google.common.io.Files;

public class Main {
	
	private static boolean isAppDir(Shell shell) {
		final File build = new File(shell.getWorkingDir(), "build.gradle");
		final Pattern pattern = Pattern.compile("^apply\\s+plugin\\s*:\\s*('|\")axelor-app('|\")$");
		try {
			for (String line : Files.readLines(build, Charset.defaultCharset())) {
				Matcher matcher = pattern.matcher(line.trim());
				if (matcher.matches()) {
					return true;
				}
			}
		} catch (IOException e) {
		}
		return false;
	}
	
	private static void printHelp() {
		System.out.println("Usage: axelor [--help] [--new <NAME>]");
		System.out.println("Run the interactive shell or create a new axelor project.");
		System.out.println();
		System.out.println("  -h, --help          show this help and exit");
		System.out.println("  -v, --version       display version information");
		System.out.println("      --new <NAME>    create a new application project");
		System.out.println();
		System.out.println("You can also execute shell commands directly like:");
		System.out.println();
		System.out.println("  axelor help");
		System.out.println("  axelor help run");
		System.out.println("  axelor clean");
		System.out.println("  axelor build");
		System.out.println("  axelor run -p 8000");
		System.out.println();
		System.out.println("See detailed documentation at http://docs.axelor.com/adk.");
	}
	
	public static void main(String[] args) throws Exception {
		
		final Shell shell = new Shell();
		final BuiltinCommands builtins = new BuiltinCommands(shell);
		
		shell.addCommand(builtins);
		shell.addCommand(new ProjectCommands(shell));
		shell.addCommand(new GradleCommands(shell));
		
		if (args.length > 0) {
			
			switch (args[0]) {
			case "--new":
				if (args.length > 1) {
					shell.executeCommand("new-project --name " + args[1]);
				} else {
					printHelp();
				}
				return;
			case "-v":
			case "--version":
				builtins.showAbout();
				return;
			case "-h":
			case "--help":
				printHelp();
				return;
			default:
				shell.execute(args);
				return;
			}
		}
		
		builtins.showAbout();
		
		if (!isAppDir(shell)) {
			System.out.println("This is not a axelor application!\n");
			System.out.println("Use `axelor --new <NAME>` to create a new application,");
			System.out.println("or go to existing application directory and run the command again.\n");
			return;
		}

		System.out.println("> Type \"help\" to see list of available commands.");
		System.out.println("> Type \"exit\" or Ctrl+D to leave this shell.");
		System.out.println();

		shell.run();
	}
}
