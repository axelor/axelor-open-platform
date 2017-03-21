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
package com.axelor.gradle.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import com.axelor.gradle.AxelorExtension;
import com.axelor.gradle.AxelorPlugin;
import com.axelor.tools.x2j.Generator;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.io.Files;

public class GenerateCode extends DefaultTask {

	public static final String TASK_NAME = "generateCode";
	public static final String TASK_DESCRIPTION = "Generate code for domain models from xml definitions.";
	public static final String TASK_GROUP = "Axelor";

	private static final String DIR_OUTPUT = "src-gen";
	private static final String DIR_SOURCE = "src/main/resources/domains";

	public GenerateCode() {
	}

	public static File getInputDirectory(Project project) {
		return new File(project.getProjectDir(), DIR_SOURCE);
	}

	public static File getOutputDirectory(Project project) {
		return new File(project.getBuildDir(), DIR_OUTPUT);
	}

	@Input
	public File getInputDirectory() {
		return getInputDirectory(getProject());
	}

	@OutputDirectory
	public File getOutputDirectory() {
		return getOutputDirectory(getProject());
	}

	private void generateInfo(AxelorExtension extension, List<ResolvedArtifact> artifacts) throws IOException {
		final Project project = getProject();
		final File outputPath = new File(getOutputDirectory(), "module.properties");
		try {
			outputPath.getParentFile().mkdirs();
		} catch (Exception e) {
			getLogger().info("Error generating module.properties", e);
		}

		getLogger().info("Generating: {}", outputPath.getParent());

		List<String> descriptionLines = new ArrayList<>();
		List<String> depends = new ArrayList<>();

		artifacts.forEach(artifact -> depends.add(artifact.getName()));

		String description = extension.getDescription();
		if (description == null) {
			description = project.getDescription();
		}
		if (description != null) {
			descriptionLines = Splitter.on("\n").trimResults().splitToList(description.trim());
		}

		final Set<String> installs = extension.getInstall();
		final Boolean removable = extension.getRemovable();

		final StringBuilder text = new StringBuilder();

		text.append("name = ").append(project.getName())
			.append("\n")
			.append("version = ").append(project.getVersion())
			.append("\n")
			.append("\n")
			.append("title = ").append(extension.getTitle())
			.append("\n")
			.append("description = ").append(Joiner.on("\\n").join(descriptionLines))
			.append("\n");

		if (removable == Boolean.TRUE) {
			text.append("\n")
				.append("removable = true")
				.append("\n");
		}
		if (!depends.isEmpty()) {
			text.append("\n")
				.append("depends = ").append(Joiner.on(", ").join(depends))
				.append("\n");
		}
		if (installs != null && !installs.isEmpty()) {
			text.append("\n")
				.append("installs = ").append(Joiner.on(", ").join(installs))
				.append("\n");
		}

		Files.write(text, outputPath, Charset.forName("UTF-8"));
	}

	private Project findProject(ResolvedArtifact artifact) {
		final ComponentIdentifier cid = artifact.getId().getComponentIdentifier();
		if (cid instanceof ProjectComponentIdentifier) {
			return getProject().findProject(((ProjectComponentIdentifier) cid).getProjectPath());
		}
		return null;
	}

	private boolean isAxelorModule(ResolvedArtifact artifact) {
		final Project sub = findProject(artifact);
		if (sub == null) {
			try (JarFile jar = new JarFile(artifact.getFile())) {
				if (jar.getEntry("module.properties") != null) {
					return true;
				}
			} catch (IOException e) {
			}
			return false;
		}
		return sub.getPlugins().hasPlugin(AxelorPlugin.class);
	}

	private void sortArtifacts(ResolvedDependency dependency, Set<Object> visited, List<ResolvedArtifact> result) {
		if (visited.contains(dependency.getName())) {
			return;
		}
		visited.add(dependency.getName());
		final Set<ResolvedArtifact> artifacts = dependency.getModuleArtifacts();
		for (ResolvedArtifact artifact : artifacts) {
			if (isAxelorModule(artifact)) {
				for (ResolvedDependency child : dependency.getChildren()) {
					sortArtifacts(child, visited, result);
				}
				result.add(artifact);
			}
		}
	}

	private List<ResolvedArtifact> findArtifacts(Configuration config) {
		final Set<Object> visited = new LinkedHashSet<>();
		final List<ResolvedArtifact> result = new ArrayList<>();
		config.getResolvedConfiguration().getFirstLevelModuleDependencies().forEach(it -> sortArtifacts(it, visited, result));
		return result;
	}
	
	private Generator buildGenerator(Project project) {
		final File domainPath = getInputDirectory(project);
		final File targetPath = getOutputDirectory(project);
		return new Generator(domainPath, targetPath);
	}

	@TaskAction
	public void generate() throws IOException {
		final Project project = getProject();
		final AxelorExtension extension = project.getExtensions().findByType(AxelorExtension.class);
		if (extension == null) {
			return;
		}

		final Configuration config = project.getConfigurations().getByName("compile");
		final List<ResolvedArtifact> result = findArtifacts(config);

		// generate module info
		generateInfo(extension, result);

		// copy module.properties
		project.copy(copy -> {
			copy.from(new File(getOutputDirectory(), "module.properties"));
			copy.into(new File(project.getBuildDir(), "classes/main"));
		});

		// start code generation
		final Generator generator = buildGenerator(project);

		// add lookup generators
		for (ResolvedArtifact artifact : result) {
			final Project sub = findProject(artifact);
			final Generator lookup = sub == null
					? Generator.forJar(artifact.getFile())
					: buildGenerator(sub);
			generator.addLookupSource(lookup);
		}

		generator.start();
	}
}
