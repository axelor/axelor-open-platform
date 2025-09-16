/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.gradle.support;

import java.net.URI;
import java.nio.file.Path;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin;

public class PublishSupport extends AbstractSupport {

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(MavenPublishPlugin.class);
    project.afterEvaluate(this::configure);
  }

  private void configure(Project project) {
    PublishingExtension publishing = project.getExtensions().findByType(PublishingExtension.class);
    configureLibrary(project, publishing);
    configureRepository(project, publishing);
  }

  private void configureLibrary(Project project, PublishingExtension publishing) {
    final JavaPluginExtension extension =
        project.getExtensions().findByType(JavaPluginExtension.class);

    if (extension == null) {
      return;
    }

    final SourceSet main = extension.getSourceSets().findByName(SourceSet.MAIN_SOURCE_SET_NAME);
    final Jar jar = (Jar) project.getTasks().findByName(JavaPlugin.JAR_TASK_NAME);
    final TaskProvider<Jar> sourcesJar =
        project
            .getTasks()
            .register(
                "sourcesJar",
                Jar.class,
                task -> {
                  task.dependsOn(JavaPlugin.COMPILE_JAVA_TASK_NAME);
                  task.from(main.getAllSource());
                  task.setManifest(jar.getManifest());
                  task.getArchiveClassifier().set("sources");
                });

    if (project.getPlugins().hasPlugin(JavaGradlePluginPlugin.class)) {
      if (project.getPlugins().hasPlugin("com.gradle.plugin-publish")) {
        // Since version 1.0.0, Plugin Publish Plugin automatically applies Maven Publish Plugin
        // https://docs.gradle.org/current/userguide/publishing_gradle_plugins.html#plugin-publishing-plugin
        return;
      }

      publishing
          .getPublications()
          .create(
              "pluginMaven",
              MavenPublication.class,
              publication -> publication.artifact(sourcesJar));
    } else {
      publishing
          .getPublications()
          .create(
              "mavenJava",
              MavenPublication.class,
              publication -> {
                publication.from(project.getComponents().getByName("java"));
                publication.artifact(sourcesJar);
              });
    }
  }

  private void configureRepository(Project project, PublishingExtension publishing) {
    final Object mavenUser = project.findProperty("mavenUsername");
    final Object mavenPass = project.findProperty("mavenPassword");
    final Object mavenRepo = project.findProperty("mavenRepository");

    if (mavenRepo == null) {
      return;
    }

    final Path rootPath = project.getRootDir().toPath().toAbsolutePath();
    final boolean isRemote = URI.create(mavenRepo.toString()).isAbsolute();
    final Object mavenUrl = isRemote ? mavenRepo : rootPath.resolve(mavenRepo.toString()).toUri();

    publishing
        .getRepositories()
        .maven(
            maven -> {
              maven.setUrl(mavenUrl);
              if (isRemote && mavenUser != null && mavenPass != null) {
                maven.credentials(
                    auth -> {
                      auth.setUsername(mavenUser.toString());
                      auth.setPassword(mavenPass.toString());
                    });
              }
            });
  }
}
