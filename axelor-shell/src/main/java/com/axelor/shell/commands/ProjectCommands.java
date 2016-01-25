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

import groovy.text.GStringTemplateEngine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.codehaus.groovy.control.CompilationFailedException;

import com.axelor.common.ClassUtils;
import com.axelor.common.Inflector;
import com.axelor.common.StringUtils;
import com.axelor.shell.core.CommandProvider;
import com.axelor.shell.core.CommandResult;
import com.axelor.shell.core.Shell;
import com.axelor.shell.core.annotations.CliCommand;
import com.axelor.shell.core.annotations.CliOption;

public class ProjectCommands implements CommandProvider {

	private static final GStringTemplateEngine ENGINE = new GStringTemplateEngine();
	private static final Map<String, String> TEMPLATES = new HashMap<>();
	
	private static String[] DIRS = {
		"src/main/java",
		"src/main/groovy",
		"src/main/resources",
		"src/test/java",
		"src/test/groovy",
		"src/test/resources"		
	};
	
	private static final Pattern NAME_PATTERN = Pattern.compile("[a-z]+([a-z0-9-]+)[a-z0-9]");

	private Shell shell;

	public ProjectCommands(Shell shell) {
		this.shell = shell;
		
		TEMPLATES.put("templates/app/gitignore.tmpl", ".gitignore");
		TEMPLATES.put("templates/app/build.gradle.tmpl", "build.gradle");
		TEMPLATES.put("templates/app/settings.gradle.tmpl", "settings.gradle");
		TEMPLATES.put("templates/app/application.properties.tmpl", "src/main/resources/application.properties");
		TEMPLATES.put("templates/app/persistence.xml.tmpl", "src/main/resources/META-INF/persistence.xml");
		TEMPLATES.put("templates/app/log4j.properties.tmpl", "src/main/resources/log4j.properties");
		TEMPLATES.put("templates/app/header.txt.tmpl", "src/license/header.txt");
		//TEMPLATES.put("templates/app/ehcache.xml.tmpl", "src/main/resources/ehcache.xml");
		TEMPLATES.put("templates/app/adk.gradle.tmpl", "gradle/adk.gradle");

		// gradle wrapper
		TEMPLATES.put("templates/app/wrapper/gradlew", "gradlew");
		TEMPLATES.put("templates/app/wrapper/gradlew.bat", "gradlew.bat");
		TEMPLATES.put("templates/app/wrapper/gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.jar");
		TEMPLATES.put("templates/app/wrapper/gradle/wrapper/gradle-wrapper.properties", "gradle/wrapper/gradle-wrapper.properties");
		
		TEMPLATES.put("templates/module/build.gradle.tmpl", "build.gradle");
		TEMPLATES.put("templates/module/Entity.xml.tmpl", "src/main/resources/domains/<%= model %>.xml");
		TEMPLATES.put("templates/module/View.xml.tmpl", "src/main/resources/views/<%= model %>.xml");
		TEMPLATES.put("templates/module/Menu.xml.tmpl", "src/main/resources/views/Menu.xml");
	}
	
	public void createStandardDirs(File target) {
		for (String name : DIRS) {
			new File(target, name).mkdirs();
		}
	}
	
	private void expand(File base, String templateName, Map<String, Object> vars) throws Exception {
		
		final InputStream is = ClassUtils.getResourceStream(templateName);
		if (is == null) {
			return;
		}
		
		StringWriter nameWriter = new StringWriter();
		ENGINE.createTemplate(TEMPLATES.get(templateName)).make(vars).writeTo(nameWriter);
		
		File target = new File(base, nameWriter.toString());
		if (!target.getParentFile().exists()) {
			target.getParentFile().mkdirs();
		}

		// not a template
		if (!templateName.endsWith(".tmpl")) {
			try {
				Files.copy(is, target.toPath());
			} finally {
				is.close();
			}
			return;
		}

		try (
			Reader reader = new BufferedReader(new InputStreamReader(is));
			Writer writer = new BufferedWriter(new FileWriter(target))) {
			ENGINE.createTemplate(reader).make(vars).writeTo(writer);
		} catch (CompilationFailedException | ClassNotFoundException e) {
			throw new IOException(e);
		}
	}
	
	private String validateNameAndGetTitle(String name, String title) {
		if (StringUtils.isBlank(name) || !NAME_PATTERN.matcher(name).matches()) {
			System.err.println();
			System.err.println("invalid project name: " + name);
			System.err.println();
			return null;
		}
		if (StringUtils.isBlank(title)) {
			title = Inflector.getInstance().humanize(name);
		}
		return title;
	}
	
	@CliCommand(name = "new-project", usage = "--name <NAME>", help = "create a new application project.", hidden = true)
	public CommandResult newProject(
			@CliOption(name = "name", shortName = 'n', help = "name of the project", required = true)
			String name,
			@CliOption(name = "title", shortName = 't', help = "display name of the application")
			String title,
			@CliOption(name = "author", shortName = 'a', help = "the author/company name")
			String author
			) {
		
		title = validateNameAndGetTitle(name, title);
		if (title == null) {
			return new CommandResult(false);
		}
		
		final File target = new File(shell.getWorkingDir().getAbsolutePath(), name);
		shell.message("\nnew application will be created in %s\n", target);
		
		if (target.exists()) {
			shell.error("The directory already exists, can't create new application.\n");
			return new CommandResult(false);
		}

		if (StringUtils.isBlank(author)) {
			author = "Axelor";
		}

		final Map<String, Object> vars = new HashMap<>();
		
		vars.put("name", name);
		vars.put("title", title);
		vars.put("author", author);
		vars.put("sdkVersion", BuiltinCommands.getVersion());

		final String pgsqlName = Inflector.getInstance().dasherize(name);
		final String mysqlName = Inflector.getInstance().underscore(name);

		vars.put("pgsqlName", pgsqlName);
		vars.put("mysqlName", mysqlName);

		try {
			for (String template : TEMPLATES.keySet()) {
				if (template.startsWith("templates/app/")) {
					expand(target, template, vars);
				}
			}
			
			// finally fix permission on gradlew scripts
			File gradlew = new File(target, "gradlew");
			if (gradlew.exists()) {
				gradlew.setExecutable(true);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			shell.error("Unable to create the application.\n");
			return new CommandResult(false);
		}
		
		shell.info("OK, application created.\n\nEnjoy!\n");
		shell.setPrompt(target.toPath());

		return new CommandResult(true, target);
	}
	
	@CliCommand(name = "new-module", usage = "--name <NAME>", help = "create a new module project.")
	public CommandResult newModule(
			@CliOption(name = "name", shortName = 'n', help = "name of the module", required = true)
			String name,
			@CliOption(name = "title", shortName = 't', help = "display name of the module")
			String title
			) {
		
		title = validateNameAndGetTitle(name, title);
		if (title == null) {
			return new CommandResult(false);
		}
		
		final File target = new File(shell.getWorkingDir().getAbsolutePath(), "modules/" + name);
		shell.message("\nnew module will be created in %s\n", target);
		
		if (target.exists()) {
			shell.error("The directory already exists, can't create new module.\n");
			return new CommandResult(false);
		}

		final Map<String, Object> vars = new HashMap<>();
		
		vars.put("name", name);
		vars.put("title", title);
		
		String ns = Inflector.getInstance().dasherize(name);
		ns = ns.substring(ns.lastIndexOf('-') + 1);
		
		vars.put("namespace", ns);
		vars.put("model", "Hello" + Inflector.getInstance().camelize(ns, false));
		
		try {
			for (String template : TEMPLATES.keySet()) {
				if (template.startsWith("templates/module/")) {
					expand(target, template, vars);
				}
			}
		} catch (Exception e) {
			shell.error("Unable to create the module.\n");
			return new CommandResult(false);
		}
		
		
		shell.info("OK, module created.\n\nEnjoy!\n");
		return new CommandResult(true);
	}
}
