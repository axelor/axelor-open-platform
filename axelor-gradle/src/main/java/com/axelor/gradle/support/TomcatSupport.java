/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.gradle.support;

import com.axelor.common.FileUtils;
import com.axelor.gradle.AxelorPlugin;
import com.axelor.gradle.AxelorUtils;
import com.axelor.gradle.tasks.AbstractRunTask;
import com.axelor.gradle.tasks.TomcatRun;
import com.google.common.io.Files;
import groovy.json.JsonBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.War;

public class TomcatSupport extends AbstractSupport {

  public static final String TOMCAT_CONFIGURATION = "tomcat";

  public static final String TOMCAT_RUN_TASK = "run";

  public static final String TOMCAT_RUNNER_CONFIG = "axelor-tomcat.properties";

  public static final String GENERATE_RUNNER_TASK = "generateRunner";
  public static final String GENERATE_LAUNCHER_TASK = "generateLauncher";

  @Override
  public void apply(Project project) {

    project.getConfigurations().create(TOMCAT_CONFIGURATION);
    applyConfigurationLibs(project, TOMCAT_CONFIGURATION, TOMCAT_CONFIGURATION);

    project
        .getTasks()
        .register(
            GENERATE_RUNNER_TASK,
            task -> {
              task.dependsOn(
                  project
                      .getExtensions()
                      .getByType(JavaPluginExtension.class)
                      .getSourceSets()
                      .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                      .getRuntimeClasspath());
              task.dependsOn(project.getConfigurations().findByName(TOMCAT_CONFIGURATION));
              task.dependsOn(WarSupport.COPY_WEBAPP_TASK_NAME);
              task.setDescription("Generate axelor-tomcat.properties.");
              task.setGroup(AxelorPlugin.AXELOR_BUILD_GROUP);
              task.doLast(a -> generateRunner(project));
            });

    project
        .getTasks()
        .register(
            TOMCAT_RUN_TASK,
            TomcatRun.class,
            task -> {
              task.dependsOn(GENERATE_RUNNER_TASK);
              task.setDescription("Run application using embedded tomcat server.");
              task.setGroup(AxelorPlugin.AXELOR_APP_GROUP);
            });

    project
        .getTasks()
        .register(
            GENERATE_LAUNCHER_TASK,
            task -> {
              task.dependsOn(GENERATE_RUNNER_TASK);
              task.setDescription("Generate ide launcher configurations.");
              task.setGroup(AxelorPlugin.AXELOR_BUILD_GROUP);
              task.doLast(a -> generateLauncher(project));
            });
  }

  public static List<File> findWebapps(Project project) {
    final List<File> webapps = new ArrayList<>();
    final File webapp = new File(project.getProjectDir(), "src/main/webapp");

    if (webapp.exists()) {
      webapps.add(webapp);
    }

    // try to use linked axelor-web's webapp dir
    project.getGradle().getIncludedBuilds().stream()
        .map(it -> new File(it.getProjectDir(), "axelor-web/src/main/webapp"))
        .filter(File::exists)
        .findFirst()
        .ifPresent(webapps::add);

    // try to use linked axelor-front's dist
    project.getGradle().getIncludedBuilds().stream()
        .map(it -> new File(it.getProjectDir(), "axelor-front/dist"))
        .filter(File::exists)
        .findFirst()
        .ifPresent(webapps::add);

    final File merged =
        new File(project.getLayout().getBuildDirectory().get().getAsFile(), "webapp");
    if (merged.exists()) {
      webapps.add(merged);
    }

    return webapps;
  }

  private static Stream<File> findOutputDirs(File projectDir) {
    // eclipse or vscode
    if (FileUtils.getFile(projectDir, "bin", "main").exists()) {
      return Stream.of(FileUtils.getFile(projectDir, "bin", "main"));
    }

    // intellij
    if (FileUtils.getFile(projectDir, "out", "production").exists()) {
      return Stream.of(
          FileUtils.getFile(projectDir, "out", "production", "classes"),
          FileUtils.getFile(projectDir, "out", "production", "resources"));
    }

    // gradle
    return Stream.of(
        FileUtils.getFile(projectDir, "build", "classes", "java", "main"),
        FileUtils.getFile(projectDir, "build", "resources", "main"));
  }

  private static List<File> findOutputDirs(Project project) {
    return Stream.concat(
            project.getRootProject().getAllprojects().stream(),
            AxelorUtils.findIncludedBuildProjects(project).stream())
        .map(Project::getProjectDir)
        .flatMap(TomcatSupport::findOutputDirs)
        .collect(Collectors.toList());
  }

  private void generateRunnerConfig(Project project) {
    final Properties props = new Properties();
    final War war = (War) project.getTasks().getByName(WarPlugin.WAR_TASK_NAME);

    final List<File> extraClasses = findOutputDirs(project);
    final List<File> extraLibs =
        Optional.ofNullable(war.getClasspath())
            .map(FileCollection::getFiles)
            .orElse(Collections.emptySet())
            .stream()
            .filter(x -> x.getName().endsWith(".jar"))
            .collect(Collectors.toList());

    props.setProperty(
        "extraClasses",
        extraClasses.stream()
            .filter(File::exists)
            .map(File::getAbsolutePath)
            .collect(Collectors.joining(",")));

    props.setProperty(
        "extraLibs",
        extraLibs.stream()
            .filter(File::exists)
            .map(File::getAbsolutePath)
            .collect(Collectors.joining(",")));

    props.setProperty(
        "webapps",
        findWebapps(project).stream()
            .filter(File::exists)
            .map(File::getAbsolutePath)
            .collect(Collectors.joining(",")));

    props.setProperty(
        "baseDir",
        FileUtils.getFile(project.getLayout().getBuildDirectory().getAsFile().get(), "tomcat")
            .getAbsolutePath());
    props.setProperty("port", "8080");
    props.setProperty(
        "contextPath",
        "/" + ((War) project.getTasks().getByName("war")).getArchiveBaseName().get());

    final File target =
        FileUtils.getFile(
            project.getLayout().getBuildDirectory().getAsFile().get(),
            "tomcat",
            TOMCAT_RUNNER_CONFIG);

    // make sure to have parent dir
    target.getParentFile().mkdirs();

    try (OutputStream os = new FileOutputStream(target)) {
      props.store(os, null);
    } catch (IOException e) {
      project.getLogger().error(e.getMessage(), e);
    }
  }

  private void generateRunnerJar(Project project) {
    FileCollection classpath =
        project
            .getExtensions()
            .getByType(JavaPluginExtension.class)
            .getSourceSets()
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            .getRuntimeClasspath()
            .plus(project.getConfigurations().findByName(TOMCAT_CONFIGURATION));

    File manifest =
        FileUtils.getFile(
            project.getLayout().getBuildDirectory().get().getAsFile(),
            TOMCAT_CONFIGURATION,
            "classpath.jar");
    String mainClass = TomcatRun.MAIN_CLASS;
    AbstractRunTask.createManifestJar(manifest, classpath, mainClass);
  }

  @SuppressWarnings("unchecked")
  private void generateLauncher(Project project) {
    File launch = FileUtils.getFile(project.getProjectDir(), ".vscode", "launch.json");

    Map<String, Object> config = new LinkedHashMap<>();
    Map<String, Object> run = new LinkedHashMap<>();

    run.put("name", "Run");
    run.put("type", "java");
    run.put("request", "launch");
    run.put("projectName", project.getName());
    run.put("classPaths", List.of("${workspaceFolder}/build/tomcat/classpath.jar"));
    run.put("cwd", "${workspaceFolder}");
    run.put("mainClass", "com.axelor.tomcat.TomcatRunner");
    run.put(
        "args", List.of("--port", 8080, "--options-from", "build/tomcat/axelor-tomcat.properties"));
    run.put("preLaunchTask", "gradle: " + GENERATE_RUNNER_TASK);

    if (launch.exists()) {
      Map<String, Object> json = (Map<String, Object>) new groovy.json.JsonSlurper().parse(launch);
      List<Map<String, Object>> configurations =
          (List<Map<String, Object>>) json.get("configurations");
      if (configurations == null) {
        configurations = new ArrayList<>();
      }

      if (configurations.stream().anyMatch(x -> "Run".equals(x.get("name")))) {
        return;
      }

      config = new HashMap<>(json);
      configurations = new ArrayList<>(configurations);
      configurations.add(run);
      config.put("configurations", configurations);
    } else {
      config.put("version", "0.2.0");
      config.put("configurations", List.of(run));
    }

    try {
      Files.createParentDirs(launch);
      Files.asCharSink(launch, StandardCharsets.UTF_8)
          .write(new JsonBuilder(config).toPrettyString());
    } catch (IOException e) {
      throw new GradleException("Unable to generate vscode launcher.", e);
    }
  }

  private void generateRunner(Project project) {
    generateRunnerConfig(project);
    generateRunnerJar(project);
  }
}
