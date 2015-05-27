/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.axelor.shell.core.annotations.CliCommand;
import com.axelor.shell.core.annotations.CliOption;
import com.google.common.base.Objects;

public class Target {

	private final Method method;

	private final Object instance;
	
	private Options options;
	
	private Set<CliOption> cliOptions = new LinkedHashSet<>();
	
	private CliCommand cliCommand;
	
	public Target(Method method, Object instance) {
		this.method = method;
		this.instance = instance;
		for (Annotation[] annotations : method.getParameterAnnotations()) {
			for (Annotation annotation : annotations) {
				if (annotation instanceof CliOption) {
					cliOptions.add((CliOption) annotation);
				}
			}
		}
		
		final Class<?>[] types = method.getParameterTypes();
		if (cliOptions.size() == types.length - 1 && (types[types.length - 1].isArray())) {
			cliOptions.add(null); // variable arguments
		}
		
		cliCommand = method.getAnnotation(CliCommand.class);
	}
	
	private Options getOptions() {
		
		if (options != null) {
			return options;
		}
		
		options = new Options();
		int counter = 0;
		
		for (CliOption cliOption : cliOptions) {
			
			if (cliOption == null) { // variable arguments
				continue;
			}
			
			Class<?> type = method.getParameterTypes()[counter++];
			String name = cliOption.name();
			String shortName = "" + cliOption.shortName();
			
			Option option = new Option(shortName, cliOption.help());
			
			option.setType(type);
			option.setLongOpt(name);
			option.setRequired(cliOption.required());
			option.setArgs(1);
			
			if (!isBlank(cliOption.argName())) {
				option.setArgName(cliOption.argName());
				option.setArgs(1);
			}
			if (type == boolean.class) {
				option.setArgs(0);
			}
			
			if (type.isArray()) {
				option.setArgs(Option.UNLIMITED_VALUES);
			}
			
			options.addOption(option);
		}
		
		return options;
	}
	
	public Method getMethod() {
		return method;
	}

	public Object getInstance() {
		return instance;
	}
	
	public Set<CliOption> getCliOptions() {
		return cliOptions;
	}
	
	public CliCommand getCliCommand() {
		return cliCommand;
	}
	
	public Object[] findArguments(String[] args) {
		final List<Object> arguments = new ArrayList<>(method.getParameterTypes().length);
		final Options options = getOptions();

		final CommandLineParser lineParser = new BasicParser();
		final CommandLine cmdLine;
		try {
			cmdLine = lineParser.parse(options, args);
		} catch (ParseException e) {
			System.out.println();
			System.out.println(e.getMessage());
			System.out.println();
			return null;
		}
		
		for (CliOption cliOption : cliOptions) {
			if (cliOption == null) {
				arguments.add(cmdLine.getArgs());
				continue;
			}
			
			String key = "" + cliOption.shortName();
			if (isBlank(key)) {
				key = cliOption.name();
			}
			
			Option opt = options.getOption(key);
			
			Object value = false;
			if (opt.hasArgs()) {
				value = cmdLine.getOptionValues(key);
			} else if (opt.hasArg()) {
				value = cmdLine.getOptionValue(key);
			} else {
				value = cmdLine.hasOption(key);
			}
			
			arguments.add(value);
		}
		
		return arguments.toArray();
	}
	
	public void showHelp() {
		
		Options options = getOptions();
		
		HelpFormatter formatter = new HelpFormatter();
		formatter.setSyntaxPrefix("Usage: ");
		formatter.setWidth(80);
		
		String cmdLine = cliCommand.name() + " " + cliCommand.usage();
		String header = cliCommand.help();
		String footer = cliCommand.notes();
		
		if (!header.endsWith("\n")) {
			header = header + "\n";
		}
		
		if (footer != null && footer.trim().length() > 0 && !footer.startsWith("\n")) {
			footer = "\n" + footer;
		}
		
		formatter.printHelp(cmdLine, header, options, footer);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(31, method, instance);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Target))
			return false;
		return Objects.equal(method, ((Target) obj).method)
				&& Objects.equal(instance, ((Target) obj).instance);
	}
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("method", method)
			.toString();
	}
}
