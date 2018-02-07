/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
package com.axelor.gradle.support;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.bundling.War;

import com.axelor.common.FileUtils;
import com.axelor.gradle.AxelorPlugin;
import com.axelor.gradle.tasks.TomcatRun;

public class TomcatSupport extends AbstractSupport {

	public static final String TOMCAT_CONFIGURATION = "tomcat";

	public static final String TOMCAT_RUN_TASK = "run";
	public static final String TOMCAT_RUNNER_JAR_TASK = "runnerJar";
	public static final String TOMCAT_RUNNER_CONFIG_TASK = "runnerConfig";

	public static final String TOMCAT_RUNNER_CLASS = "com.axelor.tomcat.TomcatRunner";
	public static final String TOMCAT_RUNNER_JAR = "axelor-tomcat.jar";
	public static final String TOMCAT_RUNNER_CONFIG = "axelor-tomcat.properties";

	public static final String GENERATE_LAUNCHER_TASK = "generateLauncher";
	
	@Override
	public void apply(Project project) {

		final Configuration tomcat = project.getConfigurations().create(TOMCAT_CONFIGURATION);
		applyConfigurationLibs(project, TOMCAT_CONFIGURATION, TOMCAT_CONFIGURATION);
		
		final File baseDir = FileUtils.getFile(project.getBuildDir(), "tomcat");

		project.getTasks().create(TOMCAT_RUN_TASK, TomcatRun.class, task -> {
			task.dependsOn(TOMCAT_RUNNER_JAR_TASK);
			task.setDescription("Run application using embedded tomcat server.");
			task.setGroup(AxelorPlugin.AXELOR_APP_GROUP);
		});

		project.getTasks().create(TOMCAT_RUNNER_CONFIG_TASK, task -> {
			task.dependsOn(JavaPlugin.CLASSES_TASK_NAME);
			task.dependsOn(WarSupport.COPY_WEBAPP_TASK_NAME);
			task.dependsOn(HotswapSupport.GENERATE_HOTSWAP_CONFIG_TASK);
			task.setDescription("Generate axelor-tomcat.properties.");
			task.doLast(a -> generateConfig(project));
		});

		project.getTasks().create(TOMCAT_RUNNER_JAR_TASK, Jar.class, task -> {
			task.dependsOn(TOMCAT_RUNNER_CONFIG_TASK);
			task.setArchiveName(TOMCAT_RUNNER_JAR);
			task.setDestinationDir(baseDir);
			task.onlyIf(t -> !task.getArchivePath().exists());

			final Map<String, String> manifest = new HashMap<>();

			manifest.put("Main-Class", TOMCAT_RUNNER_CLASS);
			manifest.put("Class-Path", tomcat.getFiles().stream()
					.filter(f -> !f.getName().contains("hotswap-agent"))
					.map(f -> f.getAbsolutePath())
					.collect(Collectors.joining(" ")));

			task.getManifest().attributes(manifest);
		});
		
		project.getTasks().create(GENERATE_LAUNCHER_TASK, task -> {
			task.setDescription("Generate ide launcher configurations.");
			task.setGroup(AxelorPlugin.AXELOR_BUILD_GROUP);
			task.dependsOn(TOMCAT_RUNNER_JAR_TASK);
		});
	}
	
	public static List<File> findWebapps(Project project) {
		final List<File> webapps = new ArrayList<>();
		final File webapp = new File(project.getProjectDir(), "src/main/webapp");

		if (webapp.exists()) {
			webapps.add(webapp);
		}

		// try to use linked axelor-web's webapp dir
		project.getGradle().getIncludedBuilds().stream()
			.map(it -> new File(it.getProjectDir(), "axelor-web/src/main/webapp"))
			.filter(it -> it.exists())
			.findFirst().ifPresent(webapps::add);
		
		final File merged = new File(project.getBuildDir(), "webapp");
		if (merged.exists()) {
			webapps.add(merged);
		}

		return webapps;
	}
	
	private void generateConfig(Project project) {
		final Properties props = new Properties();
		final War war = (War) project.getTasks().getByName(WarPlugin.WAR_TASK_NAME);
		
		final List<File> extraClasses = new ArrayList<>();
		final List<File> extraLibs = new ArrayList<>();

		for (File file : war.getClasspath()) {
			if (file.isDirectory()) {
				extraClasses.add(file);
			}
			if (file.getName().endsWith(".jar")) {
				extraLibs.add(file);
			}
		}

		extraClasses.addAll(HotswapSupport.findOutputPaths(project));

		props.setProperty("extraClasses", extraClasses.stream()
				.filter(File::exists)
				.map(File::getAbsolutePath)
				.collect(Collectors.joining(",")));

		props.setProperty("extraLibs", extraLibs.stream()
				.filter(File::exists)
				.map(File::getAbsolutePath)
				.collect(Collectors.joining(",")));

		props.setProperty("webapps", findWebapps(project).stream()
				.filter(File::exists)
				.map(File::getAbsolutePath)
				.collect(Collectors.joining(",")));

		props.setProperty("baseDir", FileUtils.getFile(project.getBuildDir(), "tomcat").getAbsolutePath());
		props.setProperty("port", "8080");
		props.setProperty("contextPath", "/" + ((War) project.getTasks().getByName("war")).getBaseName());

		final File target = FileUtils.getFile(project.getBuildDir(), "tomcat", TOMCAT_RUNNER_CONFIG);

		// make sure to have parent dir
		target.getParentFile().mkdirs();

		try (OutputStream os = new FileOutputStream(target)) {
			props.store(os, null);
		} catch (IOException e) {
			project.getLogger().error(e.getMessage(), e);
		}
	}
}
