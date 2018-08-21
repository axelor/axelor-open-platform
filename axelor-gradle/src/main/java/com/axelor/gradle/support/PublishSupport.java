/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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

import java.net.URI;
import java.nio.file.Path;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
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
    final JavaPluginConvention convention =
        project.getConvention().getPlugin(JavaPluginConvention.class);
    final SourceSet main = convention.getSourceSets().findByName(SourceSet.MAIN_SOURCE_SET_NAME);
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
                  task.setClassifier("sources");
                });

    final PublishingExtension publishing =
        project.getExtensions().findByType(PublishingExtension.class);

    publishing
        .getPublications()
        .create(
            "mavenJava",
            MavenPublication.class,
            publication -> {
              publication.from(project.getComponents().getByName("java"));
              publication.artifact(sourcesJar);
            });

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
