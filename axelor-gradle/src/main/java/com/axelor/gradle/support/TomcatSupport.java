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

import com.bmuschko.gradle.tomcat.tasks.AbstractTomcatRun;
import com.bmuschko.gradle.tomcat.tasks.TomcatRun;

public class TomcatSupport extends AbstractSupport {

	@Override
	public void apply(Project project) {
		project.getPlugins().apply(com.bmuschko.gradle.tomcat.TomcatPlugin.class);

		applyConfigurationLibs(project, "tomcat", "tomcat");

		project.getTasks().withType(AbstractTomcatRun.class).all(task -> {
			task.setHttpProtocol("org.apache.coyote.http11.Http11NioProtocol");
			task.setHttpsProtocol("org.apache.coyote.http11.Http11NioProtocol");
			task.setAjpProtocol("org.apache.coyote.ajp.AjpNioProtocol");
		});

		project.getTasks().withType(TomcatRun.class).all(task -> {
			task.dependsOn(WarSupport.COPY_WEBAPP_TASK_NAME);
			task.setWebAppSourceDirectory(new File(project.getBuildDir(), "webapp"));
		});
	}
}
