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
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.bundling.War;
import org.gradle.api.tasks.util.PatternSet;

import com.axelor.common.VersionUtils;

public class WarSupport extends AbstractSupport {

	public static final String COPY_WEBAPP_TASK_NAME = "copyWebapp";

	private final String version = VersionUtils.getVersion().version;

	@Override
	public void apply(Project project) {

		project.getPlugins().apply(WarPlugin.class);
		
		project.getConfigurations().create("axelorWeb").setTransitive(false);

		// apply providedCompile dependencies
		applyConfigurationLibs(project, "provided", "providedCompile");

		// add dependency to axelor-web
		project.getDependencies().add("compile", "com.axelor:axelor-web:" + version);
		project.getDependencies().add("axelorWeb", "com.axelor:axelor-web:" + version);

		// copy webapp to root build dir
 		project.getTasks().create(COPY_WEBAPP_TASK_NAME, Copy.class, task -> {
 			task.setDestinationDir(project.getBuildDir());
 			task.into("webapp", spec -> spec.from("src/main/webapp"));
 			project.getConfigurations().getByName("axelorWeb")
 				.getFiles()
 				.stream()
 				.filter(file -> file.getName().startsWith("axelor-web"))
 				.forEach(file -> {
 					task.from(project.zipTree(file).matching(new PatternSet().include("webapp/**/*")));
 				});
 		});

		project.getTasks().withType(War.class).all(task -> task.dependsOn(COPY_WEBAPP_TASK_NAME));
		
		final War war = (War) project.getTasks().getByName(WarPlugin.WAR_TASK_NAME);
 		war.from(project.getBuildDir() + "/webapp");
 		war.exclude("node_modules", "gulpfile.js", "package.json");
 		war.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
	}
}
