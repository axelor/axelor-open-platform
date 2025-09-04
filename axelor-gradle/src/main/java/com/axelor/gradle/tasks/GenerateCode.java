/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.gradle.tasks;

import com.axelor.common.FileUtils;
import com.axelor.gradle.AxelorExtension;
import com.axelor.gradle.AxelorPlugin;
import com.axelor.gradle.AxelorUtils;
import com.axelor.tools.code.entity.EntityGenerator;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectories;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.util.PatternSet;

public class GenerateCode extends DefaultTask {

  public static final String TASK_NAME = "generateCode";
  public static final String TASK_DESCRIPTION =
      "Generate code for domain models from xml definitions.";
  public static final String TASK_GROUP = AxelorPlugin.AXELOR_BUILD_GROUP;

  private static final String DIR_INPUT_MAIN = "src/main/resources/domains";
  private static final String DIR_INPUT_TEST = "src/test/resources/domains";

  private static final String DIR_OUTPUT_MAIN_JAVA = "src-gen/main/java";
  private static final String DIR_OUTPUT_TEST_JAVA = "src-gen/test/java";

  private static final String DIR_OUTPUT_MAIN_RESOURCES = "src-gen/main/resources";

  private Function<String, String> formatter;

  public void setFormatter(Function<String, String> formatter) {
    this.formatter = formatter;
  }

  public static File getInputMainDir(Project project) {
    return new File(project.getProjectDir(), DIR_INPUT_MAIN);
  }

  public static File getInputTestDir(Project project) {
    return new File(project.getProjectDir(), DIR_INPUT_TEST);
  }

  private static File getOutputBase(Project project) {
    return project.getLayout().getBuildDirectory().get().getAsFile();
  }

  public static File getMainJavaOutputDir(Project project) {
    return new File(getOutputBase(project), DIR_OUTPUT_MAIN_JAVA);
  }

  public static File getTestJavaOutputDir(Project project) {
    return new File(getOutputBase(project), DIR_OUTPUT_TEST_JAVA);
  }

  public static File getMainResourceOutputDir(Project project) {
    return new File(getOutputBase(project), DIR_OUTPUT_MAIN_RESOURCES);
  }

  @InputFiles
  public List<File> getLookupFiles() {
    return AxelorUtils.findAxelorProjects(getProject()).stream()
        .flatMap(
            project ->
                Arrays.asList(getMainJavaOutputDir(project), getTestJavaOutputDir(project))
                    .stream())
        .collect(Collectors.toList());
  }

  @InputFiles
  public List<File> getInputDirectories() {
    return Arrays.asList(getInputMainDir(getProject()), getInputTestDir(getProject()));
  }

  @OutputDirectories
  public List<File> getOutputDirectories() {
    return Arrays.asList(
        getMainJavaOutputDir(getProject()),
        getTestJavaOutputDir(getProject()),
        getMainResourceOutputDir(getProject()));
  }

  private void generateInfo(AxelorExtension extension, List<ResolvedArtifact> artifacts)
      throws IOException {
    final Project project = getProject();
    final File outputPath =
        FileUtils.getFile(
            getMainResourceOutputDir(getProject()), "META-INF", "axelor-module.properties");
    try {
      outputPath.getParentFile().mkdirs();
    } catch (Exception e) {
      getLogger().error("Error generating axelor-module.properties", e);
    }

    getLogger().info("Generating: {}", outputPath.getParent());

    List<String> descriptionLines = new ArrayList<>();
    List<String> depends = new ArrayList<>();
    List<String> dependsMaven = new ArrayList<>();

    artifacts.forEach(
        artifact -> {
          try {
            String module = AxelorUtils.getModuleName(project, artifact);
            String group = AxelorUtils.getGroupName(project, artifact);
            depends.add(module);
            dependsMaven.add(String.format("%s:%s", group, module));
          } catch (Exception e) {
            getLogger().error("Error generating axelor-module.properties", e);
          }
        });

    String description = extension.getDescription();
    if (description == null) {
      description = project.getDescription();
    }
    if (description != null) {
      descriptionLines = Splitter.on("\n").trimResults().splitToList(description.trim());
    }

    final StringBuilder text = new StringBuilder();

    text.append("name = ")
        .append(project.getName())
        .append("\n")
        .append("mavenGroup = ")
        .append(project.getGroup())
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

    if (AxelorUtils.isAxelorApplication(project)) {
      text.append("\n").append("application = true").append("\n");
    }

    if (!depends.isEmpty()) {
      text.append("\n").append("depends = ").append(Joiner.on(", ").join(depends)).append("\n");
    }
    if (!dependsMaven.isEmpty()) {
      text.append("depends.maven = ").append(Joiner.on(", ").join(dependsMaven)).append("\n");
    }

    Files.asCharSink(outputPath, StandardCharsets.UTF_8).write(text);
  }

  private EntityGenerator buildMainGenerator(Project project) {
    final File domainPath = getInputMainDir(project);
    final File targetPath = getMainJavaOutputDir(project);
    return formatter != null
        ? new EntityGenerator(domainPath, targetPath, formatter)
        : new EntityGenerator(domainPath, targetPath);
  }

  private EntityGenerator buildTestGenerator(Project project) {
    final File domainPath = getInputTestDir(project);
    final File targetPath = getTestJavaOutputDir(project);
    return formatter != null
        ? new EntityGenerator(domainPath, targetPath, formatter)
        : new EntityGenerator(domainPath, targetPath);
  }

  @TaskAction
  public void generate() throws IOException {
    final Project project = getProject();
    final AxelorExtension extension = project.getExtensions().findByType(AxelorExtension.class);
    if (extension == null) {
      return;
    }

    List<ResolvedArtifact> axelorArtifacts = AxelorUtils.findAxelorArtifacts(getProject());

    // generate module info
    generateInfo(extension, axelorArtifacts);

    // Generate code for main sources
    generateCodeForSourceSet(project, axelorArtifacts, true);

    // Generate code for test sources
    generateCodeForSourceSet(project, axelorArtifacts, false);
  }

  private void generateCodeForSourceSet(
      Project project, List<ResolvedArtifact> axelorArtifacts, boolean isMain) throws IOException {
    final File inputDir = isMain ? getInputMainDir(project) : getInputTestDir(project);

    // Only generate if the input directory exists and contains domain files
    if (!inputDir.exists() || !inputDir.isDirectory()) {
      getLogger()
          .info(
              "Skipping {} code generation - input directory does not exist: {}",
              isMain ? "main" : "test",
              inputDir.getAbsolutePath());
      return;
    }

    // Check if there are any domain files
    File[] domainFiles = inputDir.listFiles((dir, name) -> name.endsWith(".xml"));
    if (domainFiles == null || domainFiles.length == 0) {
      getLogger()
          .info(
              "Skipping {} code generation - no domain files found in: {}",
              isMain ? "main" : "test",
              inputDir.getAbsolutePath());
      return;
    }

    getLogger()
        .info(
            "Starting {} code generation from: {}",
            isMain ? "main" : "test",
            inputDir.getAbsolutePath());

    // Start code generation
    final EntityGenerator generator =
        isMain ? buildMainGenerator(project) : buildTestGenerator(project);

    // For test generation, also add main domains as lookup source
    if (!isMain && getInputMainDir(project).exists()) {
      getLogger().debug("Adding main domains as lookup source for test generation");
      generator.addLookupSource(buildMainGenerator(project));
    }

    // Add lookup generators from dependencies
    for (ResolvedArtifact artifact : axelorArtifacts) {
      final Project sub = AxelorUtils.findProject(project, artifact);
      if (sub == null) {
        generator.addLookupSource(
            EntityGenerator.forFiles(
                project
                    .zipTree(artifact.getFile())
                    .matching(new PatternSet().include("**/domains/**"))
                    .getFiles()));
      } else {
        generator.addLookupSource(buildMainGenerator(sub));
      }
    }

    generator.start();
    getLogger().info("Completed {} code generation", isMain ? "main" : "test");
  }
}
