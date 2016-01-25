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

import java.io.PrintStream;

import com.axelor.common.VersionUtils;
import com.axelor.shell.core.CommandProvider;
import com.axelor.shell.core.Parser;
import com.axelor.shell.core.Shell;
import com.axelor.shell.core.annotations.CliCommand;

public class BuiltinCommands implements CommandProvider {

	private static String version;
	
	private Shell shell;
	
	public BuiltinCommands(Shell shell) {
		this.shell = shell;
	}
	
	public static String getVersion() {
		if (version != null) {
			return version;
		}
		try {
			version = VersionUtils.getVersion().version;
		} catch (Exception e) {
			version = "UNKNOWN";
		}
		return version;
	}

	private void banner(PrintStream out) {
		out.println();
		out.println("     _              _                ");
		out.println("    / \\   __  _____| | ___  _ __     ");
		out.println("   / _ \\  \\ \\/ / _ \\ |/ _ \\| '__|  \\\\");
		out.println("  / ___ \\  >  <  __/ | (_) | |     //");
		out.println(" /_/   \\_\\/_/\\_\\___|_|\\___/|_|       ");
		out.println("                                     ");
		out.println();
		out.println(String
				.format("Axelor Shell, version [ %s ] - by Axelor. [ http://axelor.com ]",
						getVersion()));
		out.println();
	}
	
	@CliCommand(name = "help", usage = "[COMMAND]", help = "show command usages help", priority = -1010)
	public void showHelp(String... commands) {
		Parser parser = shell.getParser();
		if (commands == null || commands.length == 0) {
			parser.printUsage();
		} else {
			parser.printUsage(commands[0]);
		}
	}

	@CliCommand(name = "about", help = "show infomation about this shell", priority = -1009)
	public void showAbout() {
		banner(System.out);
	}
	
	@CliCommand(name = "exit", help = "exit the shell", priority = -1008)
	public void exitShell() {
		shell.stop();
	}
}
