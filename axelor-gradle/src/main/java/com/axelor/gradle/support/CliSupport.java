/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.gradle.support;

import com.axelor.gradle.tasks.PrepareDistFiles;
import java.util.List;
import java.util.stream.Stream;
import org.gradle.api.Project;
import org.gradle.api.distribution.Distribution;
import org.gradle.api.distribution.DistributionContainer;
import org.gradle.api.distribution.plugins.DistributionPlugin;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.application.CreateStartScripts;
import org.gradle.api.tasks.bundling.War;

public class CliSupport extends AbstractSupport {

  private static final String PREPARE_DIST_FILES_PATH = "generated/dist";
  private static final String START_SCRIPTS_PATH = "scripts";

  public static final String CLI_PREPARE_TASK = "prepareDistFiles";

  private static final String APP_DIR = "app";
  private static final String LIB_DIR = "lib";
  private static final String BIN_DIR = "bin";

  @Override
  public void apply(Project project) {

    project.getPlugins().apply(DistributionPlugin.class);

    War war = (War) project.getTasks().getByName(WarPlugin.WAR_TASK_NAME);

    // prepare files for distribution
    TaskProvider<PrepareDistFiles> prepareDistFiles =
        project
            .getTasks()
            .register(
                CLI_PREPARE_TASK,
                PrepareDistFiles.class,
                task -> {
                  task.getOutputDir()
                      .set(
                          project
                              .getLayout()
                              .getBuildDirectory()
                              .dir(PREPARE_DIST_FILES_PATH)
                              .get());
                });

    // start script generation
    TaskProvider<CreateStartScripts> startScripts =
        project
            .getTasks()
            .register(
                "startScripts",
                CreateStartScripts.class,
                task -> configureStartScripts(project, task));

    // Distribution Plugin
    DistributionContainer distributions =
        project.getExtensions().getByType(DistributionContainer.class);
    distributions
        .named(DistributionPlugin.MAIN_DISTRIBUTION_NAME)
        .configure(
            distribution ->
                configureDistribution(project, distribution, war, prepareDistFiles, startScripts));

    // disable zip and tar dist archives till GA
    configureDistTasks(project);
  }

  private void configureDistTasks(Project project) {
    List<String> distTaskName = List.of("assembleDist", "distZip", "distTar");
    List<String> taskNames = project.getGradle().getStartParameter().getTaskNames();

    boolean distRequested = taskNames.stream().anyMatch(distTaskName::contains);

    Stream.of(project.getTasks().named("distZip"), project.getTasks().named("distTar"))
        .forEach(
            task -> {
              task.configure(act -> act.onlyIf(spec -> distRequested));
            });
  }

  private FileCollection getTomcatRunnerLibs(Project project) {
    return project
        .getConfigurations()
        .getByName(TomcatSupport.TOMCAT_CONFIGURATION)
        .getIncoming()
        .artifactView(view -> view.componentFilter(x -> true))
        .getFiles();
  }

  private void configureStartScripts(Project project, CreateStartScripts task) {
    task.setGroup("distribution");
    task.setDescription("Creates OS specific scripts to run the project as a JVM application.");
    task.setApplicationName("axelor");
    task.setOutputDir(
        project.getLayout().getBuildDirectory().dir(START_SCRIPTS_PATH).get().getAsFile());
    task.getMainClass().set("com.axelor.app.Launcher");
    task.setClasspath(getTomcatRunnerLibs(project).filter(file -> file.getName().endsWith(".jar")));
  }

  private void configureDistribution(
      Project project,
      Distribution task,
      War war,
      TaskProvider<PrepareDistFiles> prepareDistFiles,
      TaskProvider<CreateStartScripts> startScripts) {
    task.getDistributionBaseName().set(project.getName());
    task.contents(
        copySpec -> {
          // war files → APP_DIR
          copySpec.into(APP_DIR, spec -> spec.from(project.zipTree(war.getArchiveFile())));

          // tomcat runner jars → LIB_DIR
          copySpec.into(LIB_DIR, spec -> spec.from(getTomcatRunnerLibs(project)));

          // start scripts → BIN_DIR
          copySpec.into(BIN_DIR, spec -> spec.from(startScripts));

          // prepareDistFiles → root of distribution
          copySpec.from(prepareDistFiles);
        });
  }
}
