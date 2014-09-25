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
package com.axelor.shell.core;

import static com.axelor.common.StringUtils.isBlank;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jline.console.ConsoleReader;

public class Shell implements Runnable {
	
	private ConsoleReader reader;
	
	private Parser parser;
	private Executor executor;
	
	private boolean exitRequested;
	
	public Shell() {
		parser = new Parser();
		executor = new SimpleExecutor();
		reader = createConsoleReader();
	}
	
	public Parser getParser() {
		return parser;
	}
	
	public void addCommand(CommandProvider provider) {
		parser.add(provider);
	}
	
	public void stop() {
		exitRequested = true;
	}
	
	@Override
	public void run() {
		
		this.setPrompt(getWorkingDir().toPath());
		
		String line;
		try {
			while (!exitRequested && (reader != null && ((line = reader.readLine()) != null))) {
				if (isBlank(line)) {
					continue;
				}
				executeCommand(line.trim());
			}
		} catch (IOException e) {
			throw new IllegalStateException("Shell line reading failed", e);
		}
	}
	
	public CommandResult execute(String... args) {
		ParserResult result = parser.parse(args);
		if (result == null) {
			return new CommandResult(false);
		}
		if (result.getMethod() == null && result.getInstance() == null) {
			return new CommandResult(true);
		}
		try {
			return new CommandResult(true, executor.execute(result));
		} catch (Exception e) {
			e.printStackTrace();
			return new CommandResult(false, e);
		}
	}
	
	public CommandResult executeCommand(String line) {
		return execute(parser.toArgs(line));
	}
	
	private ConsoleReader createConsoleReader() {
		ConsoleReader reader;
		try {
			reader = new ConsoleReader();
		} catch (IOException e) {
			throw new IllegalStateException("Cannot start console", e);
		}
		return reader;
	}
	
	public File getWorkingDir() {
		return Paths.get(new File(".").getAbsolutePath()).normalize().toFile();
	}
	
	public void setPrompt(String prompt) {
		reader.setPrompt(prompt);
	}
	
	public void setPrompt(Path workingDir) {
		String prompt = "[" + workingDir.toFile().getName() + "]$ ";
		reader.setPrompt(prompt);
		if (Files.exists(workingDir)) {
			System.setProperty("user.dir", workingDir.toString());
		}
	}
	
	public void println(String format, Object... args) {
		System.out.println(String.format(format, args));
	}
	
	public void message(String format, Object... args) {
		println(format, args);
	}
	
	public void error(String format, Object... args) {
		println(format, args);
	}
	
	public void info(String format, Object... args) {
		println(format, args);
	}
}
