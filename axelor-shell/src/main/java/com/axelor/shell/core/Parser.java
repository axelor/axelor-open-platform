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
package com.axelor.shell.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.axelor.shell.core.annotations.CliCommand;

public class Parser {
	
	private final Map<String, Target> targets = new ConcurrentHashMap<>();
	
	public void add(CommandProvider provider) {
		
		for (Method method : provider.getClass().getMethods()) {
			CliCommand cmd = method.getAnnotation(CliCommand.class);
			if (cmd == null) {
				continue;
			}
			Target target = new Target(method, provider);
			if (target.getCliOptions().size() != method.getParameterTypes().length) {
				continue;
			}
			targets.put(cmd.name(), new Target(method, provider));
		}
	}
	
	/**
	 * Convert the given input string into arguments array.
	 * 
	 * @param input the input string
	 * @return argument array
	 */
	public String[] toArgs(String input) {
		
		final List<String> tokens = new ArrayList<>();
		final String[] args = {};
		
		int pos = 0;
		boolean quote = false;
		String token = "";
		
		while (pos < input.length()) {
			char next = input.charAt(pos++);
			if (next == ' ' && !quote) {
				tokens.add(token);
				token = "";
			} else {
				token += next;
			}
			if (next == '"') {
				quote = !quote || input.charAt(pos - 2) == '\\';
			}
		}
		tokens.add(token);
		
		return tokens.toArray(args);
	}

	public ParserResult parse(final String line) {
		final String input = line.replaceAll(" +", " ").trim();
		final String[] args = toArgs(input.trim());
		return parse(args);
	}

	public ParserResult parse(final String[] args) {
		
		if (args == null || args.length == 0) {
			return new ParserResult(null, null);
		}
		
		final String command = args[0];
		final Target target = targets.get(command);
		
		if (target == null) {
			return null;
		}
		
		final Annotation[][] annotations = target.getMethod().getParameterAnnotations();
		
		// if no argument method
		if (annotations.length == 0) {
			return new ParserResult(target.getMethod(), target.getInstance());
		}
		
		final String[] params = Arrays.copyOfRange(args, 1, args.length);
		final Object[] arguments = target.findArguments(params);
		
		if (arguments == null) {
			return null;
		}

		return new ParserResult(target.getMethod(), target.getInstance(), arguments);
	}
	
	public void printUsage() {
		
		List<String> commands = new ArrayList<>();
		int leftWidth = 0;
		
		for (Target target : targets.values()) {
			CliCommand attrs = target.getCliCommand();
			if (attrs.hidden()) {
				continue;
			}
			String name = attrs.name();
			if (name.length() > leftWidth) {
				leftWidth = name.length();
			}
			commands.add(name);
		}
		
		Collections.sort(commands, new Comparator<String>() {
			
			@Override
			public int compare(String o1, String o2) {
				Target t1 = targets.get(o1);
				Target t2 = targets.get(o2);
				CliCommand c1 = t1.getCliCommand();
				CliCommand c2 = t2.getCliCommand();
				if (c1.priority() == c2.priority()) {
					return o1.compareTo(o2);
				}
				return c1.priority() - c2.priority();
			}
		});
		
		System.out.println();
		System.out.println("Available commands");
		System.out.println("------------------\n");
		
		for (String name : commands) {
			String help = targets.get(name).getCliCommand().help();
			if (help == null) {
				help = "";
			}
			System.out.println(String.format("  %-" + leftWidth + "s    %s", name, help));
		}
		
		System.out.println();
		System.out.println("type 'help <command>' to see help about a specific command.\n");
	}
	
	public void printUsage(String command) {
		Target target = targets.get(command);
		if (target == null) {
			System.err.println("no such command found: " + command);
			return;
		}
		System.out.println();
		target.showHelp();
		System.out.println();
	}
}
