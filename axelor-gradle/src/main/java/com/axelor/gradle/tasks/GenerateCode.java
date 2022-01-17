/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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

import com.axelor.gradle.AppPlugin;
import com.axelor.gradle.AxelorExtension;
import com.axelor.gradle.AxelorPlugin;
import com.axelor.tools.x2j.Generator;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectories;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.util.PatternSet;

public class GenerateCode extends ModuleTask {

  public static final String TASK_NAME = "generateCode";
  public static final String TASK_DESCRIPTION =
      "Generate code for domain models from xml definitions.";
  public static final String TASK_GROUP = AxelorPlugin.AXELOR_BUILD_GROUP;

  private static final String DIR_INPUT = "src/main/resources/domains";

  private static final String DIR_OUTPUT_JAVA = "src-gen/java";
  private static final String DIR_OUTPUT_RESOURCES = "src-gen/resources";

  private Function<String, String> formatter;

  public void setFormatter(Function<String, String> formatter) {
    this.formatter = formatter;
  }

  public static File getInputDir(Project project) {
    return new File(project.getProjectDir(), DIR_INPUT);
  }

  private static File getOutputBase(Project project) {
    return project.getBuildDir();
  }

  public static File getJavaOutputDir(Project project) {
    return new File(getOutputBase(project), DIR_OUTPUT_JAVA);
  }

  public static File getResourceOutputDir(Project project) {
    return new File(getOutputBase(project), DIR_OUTPUT_RESOURCES);
  }

  @InputFiles
  public List<File> getLookupFiles() {
    return moduleArtifacts().stream()
        .map(artifact -> findProject(artifact))
        .filter(Objects::nonNull)
        .map(sub -> getJavaOutputDir(sub))
        .collect(Collectors.toList());
  }

  @InputFiles
  public File getInputDirectory() {
    return getInputDir(getProject());
  }

  @OutputDirectories
  public List<File> getOutputDirectories() {
    return Arrays.asList(getJavaOutputDir(getProject()), getResourceOutputDir(getProject()));
  }

  private void generateInfo(AxelorExtension extension, List<ResolvedArtifact> artifacts)
      throws IOException {
    final Project project = getProject();
    final File outputPath = new File(getResourceOutputDir(getProject()), "module.properties");
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

    text.append("name = ")
        .append(project.getName())
        .append("\n")
        .append("version = ")
        .append(project.getVersion())
        .append("\n")
        .append("\n")
        .append("title = ")
        .append(extension.getTitle())
        .append("\n")
        .append("description = ")
        .append(Joiner.on("\\n").join(descriptionLines))
        .append("\n");

    if (project.getPlugins().hasPlugin(AppPlugin.class)) {
      text.append("\n").append("application = true").append("\n");
    }

    if (removable == Boolean.TRUE) {
      text.append("\n").append("removable = true").append("\n");
    }
    if (!depends.isEmpty()) {
      text.append("\n").append("depends = ").append(Joiner.on(", ").join(depends)).append("\n");
    }
    if (installs != null && !installs.isEmpty()) {
      text.append("\n").append("installs = ").append(Joiner.on(", ").join(installs)).append("\n");
    }

    Files.asCharSink(outputPath, Charsets.UTF_8).write(text);
  }

  private Generator buildGenerator(Project project) {
    final File domainPath = getInputDir(project);
    final File targetPath = getJavaOutputDir(project);
    return formatter != null
        ? new Generator(domainPath, targetPath, formatter)
        : new Generator(domainPath, targetPath);
  }

  @TaskAction
  public void generate() throws IOException {
    final Project project = getProject();
    final AxelorExtension extension = project.getExtensions().findByType(AxelorExtension.class);
    if (extension == null) {
      return;
    }

    // generate module info
    generateInfo(extension, moduleArtifacts());

    // start code generation
    final Generator generator = buildGenerator(project);

    // add lookup generators
    for (ResolvedArtifact artifact : moduleArtifacts()) {
      final Project sub = findProject(artifact);
      if (sub == null) {
        generator.addLookupSource(
            Generator.forFiles(
                project
                    .zipTree(artifact.getFile())
                    .matching(new PatternSet().include("**/domains/**"))
                    .getFiles()));
      } else {
        generator.addLookupSource(buildGenerator(sub));
      }
    }

    generator.start();
  }
}
