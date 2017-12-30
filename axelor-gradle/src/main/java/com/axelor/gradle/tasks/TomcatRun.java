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
import java.util.function.Function;

import org.gradle.api.Project;
import org.gradle.api.internal.tasks.options.Option;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.War;
import org.gradle.jvm.tasks.Jar;

import com.axelor.common.FileUtils;
import com.axelor.gradle.support.HotswapSupport;
import com.axelor.gradle.support.TomcatSupport;
import com.google.common.base.Joiner;

public class TomcatRun extends JavaExec {

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
		final War war = (War) project.getTasks().getByName(WarPlugin.WAR_TASK_NAME);

		final File baseDir = FileUtils.getFile(project.getBuildDir(), "tomcat");
		final File warDir = FileUtils.getFile(baseDir, "webapps", "exploded");

		final List<String> webapps = new ArrayList<>();
		final List<String> classes = new ArrayList<>();
		
		final Function<File, String> toRelativePath = path -> TomcatSupport.toRelativePath(project, path);
		
		final File webapp = new File(project.getProjectDir(), "src/main/webapp");
		if (webapp.exists()) {
			webapps.add(toRelativePath.apply(webapp));
		}

		// try to use linked axelor-web's webapp dir
		project.getGradle().getIncludedBuilds().stream()
			.map(it -> new File(it.getProjectDir(), "axelor-web/src/main/webapp"))
			.filter(it -> it.exists())
			.findFirst().ifPresent(dir -> webapps.add(toRelativePath.apply(dir)));

		// finally add exploded war
		webapps.add(toRelativePath.apply(warDir));

		// find IDE output folders
		HotswapSupport.findOutputPaths(project).stream().map(toRelativePath).forEach(classes::add);

		final List<String> args = new ArrayList<>();

		args.add("--port");
		args.add("" + port);
		args.add("--base-dir");
		args.add(toRelativePath.apply(baseDir));
		args.add("--context-path");
		args.add(war.getBaseName());
		args.add("--extra-classes");
		args.add(Joiner.on(",").join(classes));
		args.addAll(webapps);

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
		setArgs(getArgs(project, port));
		setJvmArgs(getJvmArgs(project, hot, getDebug()));
		setClasspath(((Jar) project.getTasks().getByName(TomcatSupport.TOMCAT_RUNNER_JAR_TASK)).getOutputs().getFiles());
		setMain(TomcatSupport.TOMCAT_RUNNER_CLASS);
		super.exec();
	}
}
