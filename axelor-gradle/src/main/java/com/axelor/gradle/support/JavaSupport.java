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

import java.util.ArrayList;
import org.gradle.api.Project;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.GroovyPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.GroovySourceSet;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;

public class JavaSupport extends AbstractSupport {

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(JavaPlugin.class);

    project
        .getConfigurations()
        .all(
            config -> {
              config.resolutionStrategy(
                  strategy -> {
                    strategy.preferProjectModules();
                  });
            });

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
    final JavaPluginConvention convention =
        project.getConvention().getPlugin(JavaPluginConvention.class);
    final SourceSet main = convention.getSourceSets().findByName(SourceSet.MAIN_SOURCE_SET_NAME);
    final SourceSet test = convention.getSourceSets().findByName(SourceSet.TEST_SOURCE_SET_NAME);
    final GroovySourceSet mainGroovy =
        new DslObject(main).getConvention().getPlugin(GroovySourceSet.class);
    final GroovySourceSet testGroovy =
        new DslObject(test).getConvention().getPlugin(GroovySourceSet.class);
    mainGroovy.getGroovy().srcDirs(main.getJava().getSrcDirs());
    testGroovy.getGroovy().srcDirs(test.getJava().getSrcDirs());
    main.getJava().setSrcDirs(new ArrayList<>());
    test.getJava().setSrcDirs(new ArrayList<>());
  }
}
