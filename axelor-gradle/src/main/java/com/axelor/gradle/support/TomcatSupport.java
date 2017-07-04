/*
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
package com.axelor.gradle.support;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.bundling.Jar;

import com.axelor.gradle.AxelorPlugin;
import com.axelor.gradle.tasks.TomcatRun;

public class TomcatSupport extends AbstractSupport {

	public static final String TOMCAT_CONFIGURATION = "tomcat";
	public static final String TOMCAT_RUN_TASK = "run";
	public static final String TOMCAT_RUNNER_TASK = "runnerJar";
	public static final String TOMCAT_RUNNER_CLASS = "com.axelor.tomcat.TomcatRunner";
	public static final String TOMCAT_RUNNER_JAR = "axelor-tomcat.jar";

	public static final String GENERATE_LAUNCHER_TASK = "generateLauncher";

	@Override
	public void apply(Project project) {

		final Configuration tomcat = project.getConfigurations().create(TOMCAT_CONFIGURATION);
		applyConfigurationLibs(project, TOMCAT_CONFIGURATION, TOMCAT_CONFIGURATION);

		project.getTasks().create(TOMCAT_RUN_TASK, TomcatRun.class, task -> {
			task.dependsOn(TOMCAT_RUNNER_TASK);
			task.setDescription("Run application using embedded tomcat server.");
			task.setGroup(AxelorPlugin.AXELOR_APP_GROUP);
		});

		project.getTasks().create(TOMCAT_RUNNER_TASK, Jar.class, task -> {
			task.dependsOn(JavaPlugin.CLASSES_TASK_NAME);
			task.dependsOn(WarSupport.COPY_WEBAPP_TASK_NAME);
			task.setArchiveName(TOMCAT_RUNNER_JAR);
			task.setDestinationDir(new File(project.getBuildDir(), "tomcat"));
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
			task.dependsOn(TOMCAT_RUNNER_TASK);
		});
	}
}
