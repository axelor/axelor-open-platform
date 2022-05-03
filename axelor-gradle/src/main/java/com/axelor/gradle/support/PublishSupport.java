/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
import org.gradle.jvm.tasks.Jar;

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
    final Jar sourcesJar =
        project
            .getTasks()
            .create(
                "sourcesJar",
                Jar.class,
                task -> {
                  task.dependsOn(JavaPlugin.COMPILE_JAVA_TASK_NAME);
                  task.from(main.getAllSource());
                  task.setManifest(jar.getManifest());
                  task.getArchiveClassifier().set("sources");
                });

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
