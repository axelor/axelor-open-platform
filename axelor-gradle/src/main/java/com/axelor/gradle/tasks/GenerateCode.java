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
import java.util.List;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectories;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.util.PatternSet;

public class GenerateCode extends DefaultTask {

  public static final String MAIN_TASK_NAME = "generateCode";
  public static final String TEST_TASK_NAME = "generateTestCode";
  public static final String TASK_DESCRIPTION =
      "Generate code for domain models from xml definitions.";
  public static final String TASK_GROUP = AxelorPlugin.AXELOR_BUILD_GROUP;

  // Input directory of the domain models

  private static final String DIR_INPUT_MAIN = "src/main/resources/domains";
  private static final String DIR_INPUT_TEST = "src/test/resources/domains";

  // Output directories of the domain models

  private static final String DIR_OUTPUT_MAIN_JAVA = "src-gen/main/java";
  private static final String DIR_OUTPUT_TEST_JAVA = "src-gen/test/java";

  // Output directory for the axelor-module.properties file

  private static final String DIR_OUTPUT_MAIN_RESOURCES = "src-gen/main/resources";

  /** Indicates whether test domain models should be used in the code generation process. */
  private Boolean useTestSources = Boolean.FALSE;

  /**
   * Indicates whether test domain models should be used in the code generation process.
   *
   * @return true if test domain models should be generated
   */
  @Input
  public Boolean getUseTestSources() {
    return useTestSources;
  }

  /**
   * Sets whether test domain models should be used in the code generation process.
   *
   * @param useTestSources true if test domain models should be generated
   */
  public void setUseTestSources(Boolean useTestSources) {
    this.useTestSources = useTestSources;
  }

  /**
   * Retrieves the directory containing the main domain models for the given project.
   *
   * @param project the project
   * @return the directory containing the main domain models as a {@link File}
   */
  public static File getInputMainDir(Project project) {
    return new File(project.getProjectDir(), DIR_INPUT_MAIN);
  }

  /**
   * Retrieves the directory containing the test domain models for the given project.
   *
   * @param project the project
   * @return the directory containing the test domain models as a {@link File}
   */
  public static File getInputTestDir(Project project) {
    return new File(project.getProjectDir(), DIR_INPUT_TEST);
  }

  /**
   * Retrieves the build directory of the given project
   *
   * @param project the project
   * @return the build directory as a {@link File}
   */
  private static File getOutputBase(Project project) {
    return project.getLayout().getBuildDirectory().get().getAsFile();
  }

  /**
   * Retrieves the output directory of the main domain models for the given project.
   *
   * @param project the project
   * @return the output directory of the main domain models as a {@link File}
   */
  public static File getMainJavaOutputDir(Project project) {
    return new File(getOutputBase(project), DIR_OUTPUT_MAIN_JAVA);
  }

  /**
   * Retrieves the output directory of the test domain models for the given project.
   *
   * @param project the project
   * @return the output directory of the test domain models as a {@link File}
   */
  public static File getTestJavaOutputDir(Project project) {
    return new File(getOutputBase(project), DIR_OUTPUT_TEST_JAVA);
  }

  /**
   * Retrieves the output directory of the main resources for the given project.
   *
   * @param project the project
   * @return the output directory of the main resources as a {@link File}
   */
  public static File getMainResourceOutputDir(Project project) {
    return new File(getOutputBase(project), DIR_OUTPUT_MAIN_RESOURCES);
  }

  /**
   * Retrieves a list of lookup files that are used during the code generation process. For test, an
   * empty list is returned as it is not allowed to enhance test domain models from other modules or
   * current project main domain models; otherwise, it collects the output directory of the main
   * domain models for all axelor projects associated with the current project.
   *
   * <p>Important: Gradle uses this method for incremental builds and configuration cache. It is not
   * explicitly used in the code.
   *
   * @return a list of {@link File} objects representing the lookup files
   */
  @InputFiles
  public List<File> getLookupFiles() {
    if (useTestSources) {
      return new ArrayList<>();
    }
    return AxelorUtils.findAxelorProjects(getProject()).stream()
        .map(GenerateCode::getMainJavaOutputDir)
        .collect(Collectors.toList());
  }

  /**
   * Retrieves the input directory used for the code generation process.
   *
   * <p>Important: Gradle uses this method for incremental builds and configuration cache. It is not
   * explicitly used in the code.
   *
   * @return a {@link File} object representing the input directory
   */
  @InputFiles
  public File getInputDirectory() {
    return useTestSources ? getInputTestDir(getProject()) : getInputMainDir(getProject());
  }

  /**
   * Retrieves a list of output directories for the code generation process.
   *
   * <p>Important: Gradle uses this method for incremental builds and configuration cache. It is not
   * explicitly used in the code.
   *
   * @return a list of {@link File} objects representing the output directories
   */
  @OutputDirectories
  public List<File> getOutputDirectories() {
    return useTestSources
        ? List.of(getTestJavaOutputDir(getProject()))
        : List.of(getMainJavaOutputDir(getProject()), getMainResourceOutputDir(getProject()));
  }

  /**
   * Generates the "axelor-module.properties" file for the given project.
   *
   * @param extension an instance of {@link AxelorExtension} containing module-specific metadata
   * @param artifacts a list of {@link ResolvedArtifact} objects representing the project's resolved
   *     dependencies
   * @throws IOException if an error occurs while writing to the output file
   */
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

  /**
   * Builds and returns an {@link EntityGenerator} for the main domain models of the given project.
   *
   * @param project the project
   * @return an instance of {@link EntityGenerator} configured for the main domain models of the
   *     project
   */
  private EntityGenerator buildMainGenerator(Project project) {
    final File domainPath = getInputMainDir(project);
    final File targetPath = getMainJavaOutputDir(project);
    return new EntityGenerator(domainPath, targetPath);
  }

  /**
   * Builds and returns an {@link EntityGenerator} for the test domain models of the given project.
   *
   * @param project the project
   * @return an instance of {@link EntityGenerator} configured for the test domain models of the
   *     project
   */
  private EntityGenerator buildTestGenerator(Project project) {
    final File domainPath = getInputTestDir(project);
    final File targetPath = getTestJavaOutputDir(project);
    return new EntityGenerator(domainPath, targetPath);
  }

  /**
   * Executes the code generation task.
   *
   * @throws IOException if an error occurs during file I/O operations
   */
  @TaskAction
  public void generate() throws IOException {
    final Project project = getProject();
    final AxelorExtension extension = project.getExtensions().findByType(AxelorExtension.class);
    if (extension == null) {
      return;
    }

    List<ResolvedArtifact> axelorArtifacts = AxelorUtils.findAxelorArtifacts(getProject());

    // generate module info
    if (!useTestSources) {
      generateInfo(extension, axelorArtifacts);
    }

    // Generate code for sources
    generateCodeForSourceSet(project, axelorArtifacts);
  }

  /**
   * Generates code for the given source set of the given project.
   *
   * @param project the project for which code generation is being performed
   * @param axelorArtifacts a list of resolved artifacts representing dependencies used for code
   *     generation
   * @throws IOException if an I/O error occurs during the code generation process
   */
  private void generateCodeForSourceSet(Project project, List<ResolvedArtifact> axelorArtifacts)
      throws IOException {
    final File inputDir = useTestSources ? getInputTestDir(project) : getInputMainDir(project);

    getLogger()
        .info(
            "Starting {} code generation from: {}",
            useTestSources ? "test" : "main",
            inputDir.getAbsolutePath());

    // Start code generation
    final EntityGenerator generator =
        !useTestSources ? buildMainGenerator(project) : buildTestGenerator(project);

    if (!useTestSources) {
      // Add lookup generators from dependencies. Allow extending domain models from other modules
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
    }

    generator.start();
    getLogger().info("Completed {} code generation", useTestSources ? "test" : "main");
  }
}
