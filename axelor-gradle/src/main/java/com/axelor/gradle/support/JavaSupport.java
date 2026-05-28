/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.gradle.support;

import java.util.ArrayList;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.gradle.api.plugins.GroovyPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.GroovySourceDirectorySet;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;

public class JavaSupport extends AbstractSupport {

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(JavaPlugin.class);

    project
        .getConfigurations()
        .all(config -> config.resolutionStrategy(ResolutionStrategy::preferProjectModules));

    project
        .getTasks()
        .withType(JavaCompile.class)
        .all(
            task -> {
              if (task.getOptions().getEncoding() == null) {
                task.getOptions().setEncoding("UTF-8");
              }
            });

    // force groovy compiler
    if (project.getPlugins().hasPlugin(GroovyPlugin.class)) {
      project.afterEvaluate(this::configureGroovy);
    }
  }

  private void configureGroovy(Project project) {
    final JavaPluginExtension extension =
        project.getExtensions().getByType(JavaPluginExtension.class);
    final SourceSet main = extension.getSourceSets().findByName(SourceSet.MAIN_SOURCE_SET_NAME);
    final SourceSet test = extension.getSourceSets().findByName(SourceSet.TEST_SOURCE_SET_NAME);
    final GroovySourceDirectorySet mainGroovy =
        main.getExtensions().findByType(GroovySourceDirectorySet.class);
    final GroovySourceDirectorySet testGroovy =
        main.getExtensions().findByType(GroovySourceDirectorySet.class);
    mainGroovy.srcDirs(main.getJava().getSrcDirs());
    testGroovy.srcDirs(test.getJava().getSrcDirs());
    main.getJava().setSrcDirs(new ArrayList<>());
    test.getJava().setSrcDirs(new ArrayList<>());
  }
}
