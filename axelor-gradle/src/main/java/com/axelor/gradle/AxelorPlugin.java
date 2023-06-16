/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.gradle;

import com.axelor.common.VersionUtils;
import com.axelor.gradle.support.EclipseSupport;
import com.axelor.gradle.support.IdeaSupport;
import com.axelor.gradle.support.JavaSupport;
import com.axelor.gradle.support.LicenseSupport;
import com.axelor.gradle.support.PublishSupport;
import com.axelor.gradle.support.ScriptsSupport;
import com.axelor.gradle.support.TomcatSupport;
import com.axelor.gradle.support.WarSupport;
import com.axelor.gradle.tasks.AbstractEncryptTask;
import com.axelor.gradle.tasks.EncryptFileTask;
import com.axelor.gradle.tasks.EncryptTextTask;
import com.axelor.gradle.tasks.GenerateCode;
import com.axelor.gradle.tasks.I18nTask;
import com.axelor.gradle.tasks.UpdateVersion;
import java.io.File;
import java.util.Objects;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.jvm.tasks.Jar;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.util.GradleVersion;

public class AxelorPlugin implements Plugin<Project> {

  public static final String AXELOR_APP_GROUP = "axelor application";
  public static final String AXELOR_BUILD_GROUP = "axelor build";

  public static final String GRADLE_VERSION = GradleVersion.current().getVersion();

  private final String version = VersionUtils.getVersion().version;

  public static File getClassOutputDir(Project project, String sourceType) {
    return new File(project.getBuildDir(), "classes/" + sourceType + "/main");
  }

  @Override
  public void apply(Project project) {

    project.getPlugins().apply(JavaLibraryPlugin.class);
    project.getExtensions().create(AxelorExtension.EXTENSION_NAME, AxelorExtension.class);
    project.getExtensions().create(I18nConfigExtension.EXTENSION_NAME, I18nConfigExtension.class);

    project.getPlugins().apply(JavaSupport.class);
    project.getPlugins().apply(PublishSupport.class);
    project.getPlugins().apply(LicenseSupport.class);

    if (project.getPlugins().hasPlugin(EclipsePlugin.class)) {
      project.getPlugins().apply(EclipseSupport.class);
    }
    if (project.getPlugins().hasPlugin(IdeaPlugin.class)) {
      project.getPlugins().apply(IdeaSupport.class);
    }

    configureCodeGeneration(project);
    configureJarSupport(project);
    configureWarSupport(project);
    configureEncryptionSupport(project);
  }

  private boolean isCore(Project project) {
    String name = project.getName();
    return "axelor-core".equals(name) || "axelor-web".equals(name) || "axelor-test".equals(name);
  }

  private void configureJarSupport(Project project) {
    if (isCore(project)) {
      return;
    }

    // include webapp resources in jar
    if (project != project.getRootProject()) {
      project
          .getTasks()
          .withType(Jar.class, jar -> jar.into("webapp", spec -> spec.from("src/main/webapp")));
    }

    // include core dependencies
    project.getDependencies().add("implementation", "com.axelor:axelor-core:" + version);
    project.getDependencies().add("implementation", "com.axelor:axelor-web:" + version);
    project.getDependencies().add("testImplementation", "com.axelor:axelor-test:" + version);
  }

  private void configureWarSupport(Project project) {
    // only on root project
    if (project != project.getRootProject()) return;

    project.getPlugins().apply(WarSupport.class);
    project.getPlugins().apply(ScriptsSupport.class);
    project.getPlugins().apply(TomcatSupport.class);

    // run generateCode on included builds
    AxelorUtils.findIncludedBuildProjects(project).stream()
        .map(included -> included.getTasks().findByName(GenerateCode.TASK_NAME))
        .filter(Objects::nonNull)
        .forEach(task -> project.getTasks().getByName(GenerateCode.TASK_NAME).dependsOn(task));

    // run processResources on included builds
    AxelorUtils.findIncludedBuildProjects(project).stream()
        .map(included -> included.getTasks().findByName(JavaPlugin.PROCESS_RESOURCES_TASK_NAME))
        .filter(Objects::nonNull)
        .forEach(
            task ->
                project
                    .getTasks()
                    .getByName(JavaPlugin.PROCESS_RESOURCES_TASK_NAME)
                    .dependsOn(task));
  }

  private void configureCodeGeneration(Project project) {
    project
        .getTasks()
        .create(
            I18nTask.TASK_NAME,
            I18nTask.class,
            task -> {
              task.setDescription(I18nTask.TASK_DESCRIPTION);
              task.setGroup(I18nTask.TASK_GROUP);
            });

    project
        .getTasks()
        .create(
            UpdateVersion.TASK_NAME,
            UpdateVersion.class,
            task -> {
              task.setDescription(UpdateVersion.TASK_DESCRIPTION);
              task.setGroup(UpdateVersion.TASK_GROUP);
              final ConfigurableFileTree files = project.fileTree(project.getProjectDir());
              files.include("**/resources/**/*.xml", "**/data/**/*config.xml");
              task.setProcessFiles(files);
            });

    Task compileTask = project.getTasks().findByName(JavaPlugin.COMPILE_JAVA_TASK_NAME);
    Task resourcesTask = project.getTasks().findByName(JavaPlugin.PROCESS_RESOURCES_TASK_NAME);
    Task generateCodeTask =
        project
            .getTasks()
            .create(
                GenerateCode.TASK_NAME,
                GenerateCode.class,
                task -> {
                  task.setDescription(GenerateCode.TASK_DESCRIPTION);
                  task.setGroup(GenerateCode.TASK_GROUP);
                });

    // add dependencies to optimize up-to-date checks
    dependsOn(compileTask, generateCodeTask);
    dependsOn(resourcesTask, generateCodeTask);
    project.afterEvaluate(
        p -> {
          AxelorUtils.findAxelorProjects(p)
              .forEach(
                  d -> {
                    dependsOn(generateCodeTask, d.getTasks().findByName(GenerateCode.TASK_NAME));
                    dependsOn(compileTask, d.getTasks().findByName(JavaPlugin.CLASSES_TASK_NAME));
                  });
        });

    // add src-gen dirs
    project
        .getExtensions()
        .getByType(JavaPluginExtension.class)
        .getSourceSets()
        .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        .getJava()
        .srcDir(GenerateCode.getJavaOutputDir(project));
    project
        .getExtensions()
        .getByType(JavaPluginExtension.class)
        .getSourceSets()
        .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        .getResources()
        .srcDir(GenerateCode.getResourceOutputDir(project));

    // XXX: prepend class output directory to compile classpath (see #26420)
    // XXX: https://github.com/gradle/gradle/issues/12575
    project
        .getExtensions()
        .getByType(JavaPluginExtension.class)
        .getSourceSets()
        .getByName(
            SourceSet.MAIN_SOURCE_SET_NAME,
            sourceSet -> {
              sourceSet.setCompileClasspath(
                  project
                      .files(sourceSet.getJava().getClassesDirectory().get().getAsFile())
                      .plus(sourceSet.getCompileClasspath()));
            });
  }

  private void configureEncryptionSupport(Project project) {
    project
        .getTasks()
        .create(
            EncryptTextTask.TASK_NAME,
            EncryptTextTask.class,
            task -> {
              task.setDescription(EncryptTextTask.TASK_DESCRIPTION);
              task.setGroup(AbstractEncryptTask.TASK_GROUP);
            });

    project
        .getTasks()
        .create(
            EncryptFileTask.TASK_NAME,
            EncryptFileTask.class,
            task -> {
              task.setDescription(EncryptFileTask.TASK_DESCRIPTION);
              task.setGroup(AbstractEncryptTask.TASK_GROUP);
            });
  }

  private void dependsOn(Task task, Task dependency) {
    if (task != null && dependency != null) {
      task.dependsOn(dependency);
    }
  }
}
