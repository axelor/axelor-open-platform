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

import com.axelor.gradle.AxelorPlugin;
import com.axelor.gradle.tasks.DbTask;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.SourceSet;

public class ScriptsSupport extends AbstractSupport {

  @Override
  public void apply(Project project) {

    project
        .getTasks()
        .create(
            "npm-install",
            Exec.class,
            task -> {
              task.setDescription("Run 'npm install' command to install npm packages.");
              task.setGroup(AxelorPlugin.AXELOR_BUILD_GROUP);
              task.setWorkingDir(project.getBuildDir() + "/webapp");
              task.setCommandLine("npm", "install");
              task.dependsOn("copyWebapp");
            });

    project
        .getTasks()
        .create(
            "npm-build",
            Exec.class,
            task -> {
              task.setDescription("Run 'npm run build' command to build web resource bundles.");
              task.setGroup(AxelorPlugin.AXELOR_BUILD_GROUP);
              task.setWorkingDir(project.getBuildDir() + "/webapp");
              task.setCommandLine("npm", "run", "build");
              task.dependsOn("npm-install");
            });

    project
        .getTasks()
        .create(
            DbTask.TASK_NAME,
            DbTask.class,
            task -> {
              task.setDescription(DbTask.TASK_DESCRIPTION);
              task.setGroup(DbTask.TASK_GROUP);
              task.setClasspath(
                  project
                      .getConvention()
                      .getPlugin(JavaPluginConvention.class)
                      .getSourceSets()
                      .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                      .getRuntimeClasspath());
            });
  }
}
