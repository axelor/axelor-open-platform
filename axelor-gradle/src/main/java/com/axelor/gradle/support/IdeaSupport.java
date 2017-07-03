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
import java.io.IOException;

import org.gradle.api.Project;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModel;

import com.axelor.gradle.AppPlugin;
import com.axelor.gradle.AxelorPlugin;
import com.axelor.gradle.tasks.GenerateCode;
import com.axelor.gradle.tasks.TomcatRun;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Files;

public class IdeaSupport extends AbstractSupport {

	@Override
	public void apply(Project project) {
		project.getPlugins().apply(IdeaPlugin.class);
		project.afterEvaluate(p -> {
			if (project.getPlugins().hasPlugin(AxelorPlugin.class)) {
				project.getTasks().getByName("ideaModule").dependsOn(GenerateCode.TASK_NAME);
				project.getConvention().getByType(IdeaModel.class).getModule()
					.getGeneratedSourceDirs().add(GenerateCode.getOutputDirectory(project));
			}
			if (project.getPlugins().hasPlugin(AppPlugin.class)) {
				project.getTasks().getByName("ideaModule").doLast(task -> generateLauncher(project));
			}
		});
	}

	private void generateLauncher(Project project) {
		final String name = String.format("%s (run)", project.getName());
		final String output = String.format(".idea/runConfigurations/%s__run_.xml", project.getName());

		final TomcatRun tomcatRun = (TomcatRun) project.getTasks().getByName(TomcatSupport.TOMCAT_RUN_TASK);

		// configure tomcatRun
		tomcatRun.configure(false, true);
		
		final StringBuilder builder = new StringBuilder();
		builder.append("<component name='ProjectRunConfigurationManager'>\n");
		builder.append("  <configuration default='false' name='").append(name).append("'")
			.append(" type='JarApplication' factoryName='JAR Application' singleton='true'>\n");
		builder.append("    <option name='JAR_PATH' value='$PROJECT_DIR$/build/tomcat/axelor-tomcat.jar' />\n");
		builder.append("    <option name='VM_PARAMETERS' value='")
			.append(Joiner.on(" ").join(tomcatRun.getJvmArgs())).append("' />\n");
		builder.append("    <option name='PROGRAM_PARAMETERS' value='")
			.append(Joiner.on(" ").join(tomcatRun.getArgs())).append("' />\n");
		builder.append("    <option name='WORKING_DIRECTORY' value='$PROJECT_DIR$' />\n");
		builder.append("    <option name='ALTERNATIVE_JRE_PATH' />\n");
		builder.append("    <envs />\n");
		builder.append("    <module name='").append(project.getName()).append("' />\n");
		builder.append("    <method>\n");
		builder.append("      <option name='Gradle.BeforeRunTask' enabled='true' tasks='runnerJar' externalProjectPath='$PROJECT_DIR$' vmOptions='' scriptParameters='' />\n");
		builder.append("    </method>\n");
		builder.append("  </configuration>\n");
		builder.append("</component>\n");
		
		File out = new File(project.getProjectDir(), output);
		try {
			Files.createParentDirs(out);
			Files.write(builder, out, Charsets.UTF_8);
		} catch (IOException e) {
		}
	}
}
