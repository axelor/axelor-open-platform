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
package com.axelor.gradle.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.internal.tasks.options.Option;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import com.axelor.common.FileUtils;
import com.axelor.gradle.support.HotswapSupport;
import com.axelor.gradle.support.TomcatSupport;

public class TomcatRun extends JavaExec {

	private static final String MAIN_CLASS = "com.axelor.app.internal.AppRunner";

	private boolean hot;

	private int port = 8080;

	@Option(option = "hot", description = "Specify whether to enable hot-swaping.")
	public void setHot(boolean hot) {
		this.hot = hot;
	}

	@Option(option = "port", description = "Specify the tomcat server port.")
	public void setPort(String port) {
		this.port = Integer.parseInt(port);
	}

	public static List<String> getArgs(Project project, int port) {
		final File baseDir = FileUtils.getFile(project.getBuildDir(), "tomcat");
		final File confFile = FileUtils.getFile(baseDir, TomcatSupport.TOMCAT_RUNNER_CONFIG);

		final List<String> args = new ArrayList<>();

		args.add("--port");
		args.add("" + port);
		args.add("--config");
		args.add(TomcatSupport.toRelativePath(project, confFile));

		return args;
	}
	
	public static List<String> getJvmArgs(Project project, boolean hot, boolean debug) {
		final List<String> jvmArgs = new ArrayList<>();
		if (hot || debug) {
			if (HotswapSupport.hasDCEVM()) {
				HotswapSupport.getAgentArgs(project, !debug).forEach(jvmArgs::add);
			} else {
				project.getLogger().info("Cannot enable hot-swaping as DCEVM is not installed.");
			}
		}
		return jvmArgs;
	}

	@TaskAction
	@Override
	public void exec() {
		final Project project = getProject();
		setMain(MAIN_CLASS);
		setArgs(getArgs(project, port));
		setJvmArgs(getJvmArgs(project, hot, getDebug()));
		setClasspath(project.getConvention()
			.getPlugin(JavaPluginConvention.class)
			.getSourceSets()
			.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
			.getRuntimeClasspath());
		super.exec();
	}
}
