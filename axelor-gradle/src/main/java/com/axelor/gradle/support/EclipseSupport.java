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
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.EclipseWtpPlugin;
import org.gradle.plugins.ide.eclipse.model.AccessRule;
import org.gradle.plugins.ide.eclipse.model.Classpath;
import org.gradle.plugins.ide.eclipse.model.Container;
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.eclipse.model.Library;
import org.gradle.plugins.ide.eclipse.model.ProjectDependency;
import org.gradle.plugins.ide.eclipse.model.SourceFolder;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.axelor.common.FileUtils;
import com.axelor.gradle.AppPlugin;
import com.axelor.gradle.AxelorPlugin;
import com.axelor.gradle.tasks.GenerateCode;
import com.axelor.gradle.tasks.TomcatRun;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Files;
import com.google.common.xml.XmlEscapers;

public class EclipseSupport extends AbstractSupport {

	@Override
	public void apply(Project project) {
		project.getPlugins().apply(EclipsePlugin.class);
		project.getPlugins().apply(EclipseWtpPlugin.class);

		if (project.getPlugins().hasPlugin(AxelorPlugin.class)) {
			project.getTasks().getByName(EclipsePlugin.ECLIPSE_CP_TASK_NAME).dependsOn(GenerateCode.TASK_NAME);
		}

		final EclipseModel eclipse = project.getExtensions().getByType(EclipseModel.class);
		final EclipseClasspath ecp = eclipse.getClasspath();
		
		ecp.setDefaultOutputDir(project.file("bin/main"));
		ecp.getFile().whenMerged((Classpath cp) -> {
			// separate output for main & test sources
			cp.getEntries().stream()
				.filter(it -> it instanceof SourceFolder).map(it -> (SourceFolder) it)
				.filter(it -> it.getPath().startsWith("src/main/") || it.getPath().endsWith("src-gen"))
				.forEach(it -> it.setOutput("bin/main"));
			
			cp.getEntries().stream()
				.filter(it -> it instanceof SourceFolder).map(it -> (SourceFolder) it)
				.filter(it -> it.getPath().startsWith("src/test/"))
				.forEach(it -> it.setOutput("bin/test"));
			
			// remove self-dependency
			cp.getEntries().removeIf(it -> it instanceof SourceFolder && ((SourceFolder) it).getPath().contains(project.getName()));
			cp.getEntries().removeIf(it -> it instanceof Library && ((Library) it).getPath().contains(project.getName() + "/build"));
			
			// add access rule for nashorn api
			cp.getEntries()
				.stream()
				.filter(it -> it instanceof Container).map(it -> (Container) it)
				.filter(it -> it.getPath().contains("org.eclipse.jdt.launching.JRE_CONTAINER"))
				.forEach(it -> it.getAccessRules().add(new AccessRule("0", "jdk/nashorn/api/**")));
			
			// generate launcher
			if (project.getPlugins().hasPlugin(AppPlugin.class)) {
				generateLauncher(project, cp);
			}
		});

		// finally configure wtp resources
		project.afterEvaluate(p -> {
			if (project.getPlugins().hasPlugin(AppPlugin.class)) {
				configureWtp(project, eclipse);
			}
		});
	}
	
	private Map<String, String> resource(String deployPath, String sourcePath) {
		final Map<String, String> map = new HashMap<>();
		map.put("deployPath", deployPath);
		map.put("sourcePath", sourcePath);
		return map;
	}
	
	private Map<String, String> link(String name, String location) {
		final Map<String, String> map = new HashMap<>();
		map.put("name", name);
		map.put("type", "2");
		map.put("location", location);
		return map;
	}

	private void configureWtp(Project project, EclipseModel eclipse) {
		// try to link axelor-web's webapp dir
		final File dir = project.getGradle().getIncludedBuilds().stream()
				.map(it -> new File(it.getProjectDir(), "axelor-web/src/main/webapp"))
				.filter(it -> it.exists())
				.findFirst().orElse(null);

		if (dir != null) {
			eclipse.getProject().linkedResource(link("axelor-webapp", dir.getPath()));
			eclipse.getWtp().getComponent().resource(resource("/", dir.getPath()));
			eclipse.getWtp().getComponent().getFile().withXml(provider -> {
				// XXX: fix linked resource path issue
				final NodeList nodes = provider.asElement().getElementsByTagName("wb-resource");
				for (int i = 0; i < nodes.getLength(); i++) {
					final Element n = (Element) nodes.item(i);
					if (dir.getPath().equals(n.getAttribute("source-path"))) {
						n.setAttribute("source-path", "axelor-webapp");
						break;
					}
				}
			});
		}

		// finally add build/webapp
		eclipse.getWtp().getComponent().resource(resource("/", "build/webapp"));
	}

	private void appendContainer(StringBuilder builder, String type, Object... memento) {
		builder.append("<container")
			.append(" typeId=")
			.append('"').append(type).append('"')
			.append(" memento=")
			.append('"')
				.append(XmlEscapers.xmlAttributeEscaper().escape("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"))
				.append(XmlEscapers.xmlAttributeEscaper().escape(Joiner.on("").join(memento)))
			.append('"')
			.append("/>\n");
	}

	private String generateSourceLookup(Project project, Classpath cp) {
		final StringBuilder builder = new StringBuilder();
		builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
		builder.append("<sourceLookupDirector>\n");
		builder.append("<sourceContainers duplicates=\"false\">\n");

		// default container
		appendContainer(builder,
				"org.eclipse.debug.core.containerType.default",
				"<default/>");

		// project itself
		appendContainer(builder,
				"org.eclipse.jdt.launching.sourceContainer.javaProject",
				"<javaProject name=", '"', project.getName(), '"', "/>");

		// linked projects
		cp.getEntries().stream()
			.filter(e -> e instanceof ProjectDependency)
			.map(e -> (ProjectDependency) e)
			.forEach(e -> {
				appendContainer(builder,
						"org.eclipse.jdt.launching.sourceContainer.javaProject",
						"<javaProject name=", '"', e.getPath().substring(1), '"', "/>");
			});

		// source jars
		cp.getEntries().stream()
			.filter(e -> e instanceof Library)
			.map(e -> (Library) e)
			.filter(e -> e.getSourcePath() != null)
			.filter(e -> e.getSourcePath().getFile().exists())
			.map(e -> e.getSourcePath().getFile().getAbsolutePath())
			.forEach(path -> {
				appendContainer(builder,
						"org.eclipse.debug.core.containerType.externalArchive",
						"<archive detectRoot=", '"', "true", '"', " path=", '"', path, '"', "/>");
			});

		builder.append("</sourceContainers>\n");
		builder.append("</sourceLookupDirector>\n");

		return XmlEscapers.xmlAttributeEscaper().escape(builder.toString());
	}

	private void generateLauncher(Project project, Classpath cp) {
		final StringBuilder builder = new StringBuilder();
		builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
		builder.append("<launchConfiguration type=\"org.eclipse.jdt.launching.localJavaApplication\">\n");

		builder.append("<listAttribute key=\"org.eclipse.debug.core.MAPPED_RESOURCE_PATHS\">\n")
			.append("<listEntry value=\"/").append(project.getName()).append("\"/>\n")
			.append("</listAttribute>\n");

		builder.append("<listAttribute key=\"org.eclipse.debug.core.MAPPED_RESOURCE_TYPES\">\n")
			.append("<listEntry value=\"4\"/>\n")
			.append("</listAttribute>\n");

		builder.append("<stringAttribute")
				.append(" key=\"org.eclipse.debug.core.source_locator_id\"")
				.append(" value=\"org.eclipse.jdt.launching.sourceLocator.JavaSourceLookupDirector\"")
				.append("/>\n");

		builder.append("<stringAttribute")
			.append(" key=\"org.eclipse.debug.core.source_locator_memento\"")
			.append(" value=").append('"').append(generateSourceLookup(project, cp)).append('"')
			.append("/>\n");

		builder.append("<booleanAttribute key=\"org.eclipse.jdt.launching.ATTR_USE_START_ON_FIRST_THREAD\" value=\"true\"/>\n");

		// classpath
		builder.append("<listAttribute key=\"org.eclipse.jdt.launching.CLASSPATH\">\n");
		builder.append("<listEntry value=")
			.append('"')
			.append(XmlEscapers.xmlAttributeEscaper().escape(""
				+ "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
				+ "<runtimeClasspathEntry"
				+ "	containerPath=\"org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.8/\""
				+ "	javaProject=\"" + project.getName() + "\""
				+ "	path=\"1\""
				+ "	type=\"4\"/>"
			))
			.append('"')
			.append("/>\n");
		
		final TomcatRun tomcatRun = (TomcatRun) project.getTasks().getByName(TomcatSupport.TOMCAT_RUN_TASK);

		// configure tomcatRun
		tomcatRun.configure(false, true);

		tomcatRun.getClasspath().forEach(file -> {
			builder.append("<listEntry value=")
				.append('"')
				.append(XmlEscapers.xmlAttributeEscaper().escape(""
						+ "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
						+ "<runtimeClasspathEntry"
						+ " externalArchive=\"" + file.getAbsolutePath() + "\""
						+ " path=\"3\""
						+ " type=\"2\"/>"))
				.append('"')
				.append("/>\n");
		});

		builder.append("</listAttribute>\n");

		builder.append("<booleanAttribute key=\"org.eclipse.jdt.launching.DEFAULT_CLASSPATH\" value=\"false\"/>\n");
		builder.append("<stringAttribute key=\"org.eclipse.jdt.launching.MAIN_TYPE\"")
			.append(" value=").append('"').append(tomcatRun.getMain()).append('"').append("/>\n");

		builder.append("<stringAttribute key=\"org.eclipse.jdt.launching.PROGRAM_ARGUMENTS\"")
			.append(" value=")
			.append('"')
			.append(Joiner.on(" ").join(tomcatRun.getArgs()))
			.append('"')
			.append("/>\n");

		builder.append("<stringAttribute key=\"org.eclipse.jdt.launching.PROJECT_ATTR\"")
			.append(" value=")
			.append('"')
			.append(project.getName())
			.append('"')
			.append("/>\n");

		builder.append("<stringAttribute key=\"org.eclipse.jdt.launching.VM_ARGUMENTS\"")
			.append(" value=")
			.append('"')
			.append(Joiner.on(" ").join(tomcatRun.getJvmArgs()))
			.append('"')
			.append("/>\n");

		builder.append("</launchConfiguration>");

		final File output = FileUtils.getFile(project.getProjectDir(), ".settings", String.format("%s (run).launch", project.getName()));
		try {
			Files.createParentDirs(output);
			Files.write(builder, output, Charsets.UTF_8);
		} catch (IOException e) {
		}
	}
}
