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
package com.axelor.gradle.support;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;

public class ScriptsSupport extends AbstractSupport {

	@Override
	public void apply(Project project) {

		project.getTasks().create("npm", Exec.class, task -> {
 			task.setDescription("Run 'npm install' command to install npm packages.");
 			task.setGroup("Axelor");
 			task.setWorkingDir(project.getBuildDir() + "/webapp");
 			task.setCommandLine("npm", "install");
 		});

		project.getTasks().create("gulp", Exec.class, task -> {
			task.setDescription("Run gulp command to build web resource bundles.");
			task.setGroup("Axelor");
			task.setWorkingDir(project.getBuildDir() + "/webapp");
			task.setCommandLine("npm", "run", "gulp");
		});
		
		project.getTasks().create("init", JavaExec.class, task -> {
			task.setDescription("Initialize application database.");
			task.setGroup("Axelor");
			task.setClasspath(project.getConvention()
					.getPlugin(JavaPluginConvention.class)
					.getSourceSets()
					.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
					.getRuntimeClasspath());

			if ("true".equals(project.getProperties().get("update"))) {
				task.args("-u");
			} else {
				task.args("-i");
			}
			if (project.getProperties().get("modules") != null) {
				task.args("-m", project.getProperties().get("modules"));
			}
			task.jvmArgs("-Daxelor.config=" + task.getSystemProperties().get("axelor.config"));
			task.setMain("com.axelor.app.internal.AppCli");
		});
		
		project.getTasks().create("migrate", JavaExec.class, task -> {
			task.setDescription("Run database migration scripts.");
			task.setGroup("Axelor");
			task.setClasspath(project.getConvention()
					.getPlugin(JavaPluginConvention.class)
					.getSourceSets()
					.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
					.getRuntimeClasspath());
			task.args("-M");
			if ("true".equals(project.getProperties().get("verbose"))) {
				task.args("--verbose");
			}
			task.jvmArgs("-Daxelor.config=" + task.getSystemProperties().get("axelor.config"));
			task.setMain("com.axelor.app.internal.AppCli");
		});
	}
}
