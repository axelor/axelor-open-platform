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

import java.io.File;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.bundling.War;

import com.axelor.common.FileUtils;
import com.axelor.gradle.AxelorPlugin;
import com.axelor.gradle.tasks.TomcatRun;

public class TomcatSupport extends AbstractSupport {

	public static final String TOMCAT_RUN_TASK = "run";

	public static final String TOMCAT_WEBAPP_TASK = "runWebapp";

	@Override
	public void apply(Project project) {

		project.getConfigurations().create(TomcatRun.TOMCAT_CONFIGURATION);
		applyConfigurationLibs(project, TomcatRun.TOMCAT_CONFIGURATION, TomcatRun.TOMCAT_CONFIGURATION);

		final War war = (War) project.getTasks().getByName(WarPlugin.WAR_TASK_NAME);

		final File baseDir = FileUtils.getFile(project.getBuildDir(), "tomcat");
		final File webappDir = FileUtils.getFile(baseDir, "webapps", war.getBaseName());
		
		project.getTasks().create(TOMCAT_WEBAPP_TASK, Sync.class, task -> {
			task.dependsOn(JavaPlugin.CLASSES_TASK_NAME);
			task.dependsOn(WarSupport.COPY_WEBAPP_TASK_NAME);
			task.into(FileUtils.getFile(baseDir, "webapps", war.getBaseName()));
			task.with(war);
		});

		project.getTasks().create(TOMCAT_RUN_TASK, TomcatRun.class, task -> {
			task.dependsOn(TOMCAT_WEBAPP_TASK);
			task.setGroup(AxelorPlugin.AXELOR_APP_GROUP);
			task.setBaseDir(baseDir);
			task.setWebappDir(webappDir);
		});
	}
}
