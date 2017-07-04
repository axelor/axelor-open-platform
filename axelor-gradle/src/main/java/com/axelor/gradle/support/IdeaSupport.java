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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
				project.getExtensions().getByType(IdeaModel.class).getModule()
					.getGeneratedSourceDirs().add(GenerateCode.getOutputDirectory(project));
			}
			if (project.getPlugins().hasPlugin(AppPlugin.class)) {
				final String name = String.format("%s (run)", project.getName());
				project.getTasks().getByName("ideaModule").dependsOn(WarSupport.COPY_WEBAPP_TASK_NAME);
				project.getExtensions().getByType(IdeaModel.class).getWorkspace().getIws().withXml(xmlProvider -> {
					try {
						generateLauncher(project.getRootProject(), xmlProvider.asElement(), name);
					} catch (Exception e) {
					}
				});
				project.getTasks().create("generateIdeaLauncher", task -> {
					task.onlyIf(t -> new File(project.getRootDir(), ".idea/workspace.xml").exists());
					task.doLast(t -> generateLauncher(project, name));
					Task generateLauncher = project.getTasks().getByName("generateLauncher");
					if (generateLauncher != null) {
						generateLauncher.finalizedBy(task);
					}
				});
			}
		});
	}

	private void generateLauncher(Project project, Element root, String name) throws Exception {
		final Document doc = root.getOwnerDocument();
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder builder = factory.newDocumentBuilder();
		final XPathFactory xPathfactory = XPathFactory.newInstance();
		final XPath xpath = xPathfactory.newXPath();

		final Element runManager = (Element) xpath.evaluate("//component[@name='RunManager']", root, XPathConstants.NODE);
		final Element run = (Element) xpath.evaluate("//configuration[@name='" + name + "']", runManager, XPathConstants.NODE);
		if (run != null) {
			runManager.removeChild(run);
		}
		final String code = generateRunConfiguration(project, name);
		final Node node = builder.parse(new ByteArrayInputStream(code.getBytes())).getDocumentElement();
		runManager.insertBefore(doc.importNode(node, true), runManager.getFirstChild());
	}

	private void generateLauncher(Project project, String name) {
		final String outName = String.format(".idea/runConfigurations/%s.xml", name.replaceAll("[^\\w]", "_"));
		final File outFile = new File(project.getRootDir(), outName);
		final String code = "<component name='ProjectRunConfigurationManager'>\n"
				+ generateRunConfiguration(project, name)
				+ "</component>\n";

		try {
			Files.createParentDirs(outFile);
			Files.write(code, outFile, Charsets.UTF_8);
		} catch (IOException e) {
		}
	}

	private String generateRunConfiguration(Project project, String name) {
		final TomcatRun tomcatRun = (TomcatRun) project.getTasks().getByName(TomcatSupport.TOMCAT_RUN_TASK);
		// configure tomcatRun
		tomcatRun.configure(false, true);

		final StringBuilder builder = new StringBuilder();
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
		return builder.toString();
	}
}
