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

import com.axelor.gradle.AxelorPlugin;
import com.axelor.gradle.tasks.TomcatRun;

public class TomcatSupport extends AbstractSupport {

	public static final String TOMCAT_CONFIGURATION = "tomcat";
	public static final String TOMCAT_RUN_TASK = "run";

	@Override
	public void apply(Project project) {

		project.getConfigurations().create(TOMCAT_CONFIGURATION);
		applyConfigurationLibs(project, TOMCAT_CONFIGURATION, TOMCAT_CONFIGURATION);

		project.getTasks().create(TOMCAT_RUN_TASK, TomcatRun.class, task -> {
			task.dependsOn(WarSupport.COPY_WEBAPP_TASK_NAME);
			task.setGroup(AxelorPlugin.AXELOR_APP_GROUP);
		});
	}
}
