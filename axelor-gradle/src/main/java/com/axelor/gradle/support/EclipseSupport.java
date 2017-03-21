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
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.EclipseWtpPlugin;
import org.gradle.plugins.ide.eclipse.model.Classpath;
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.eclipse.model.Library;
import org.gradle.plugins.ide.eclipse.model.SourceFolder;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.axelor.gradle.AppPlugin;
import com.axelor.gradle.AxelorPlugin;
import com.axelor.gradle.tasks.GenerateCode;

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
}
