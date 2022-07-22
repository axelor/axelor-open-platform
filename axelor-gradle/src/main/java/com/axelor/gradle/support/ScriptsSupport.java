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

import com.axelor.gradle.AxelorPlugin;
import com.axelor.gradle.tasks.DatabaseTask;
import com.github.gradle.node.NodeExtension;
import com.github.gradle.node.NodePlugin;
import com.github.gradle.node.npm.task.NpmInstallTask;
import com.github.gradle.node.npm.task.NpmTask;
import java.io.File;
import java.util.List;
import org.gradle.api.Project;
import org.gradle.api.plugins.WarPlugin;

public class ScriptsSupport extends AbstractSupport {

  public static final String DATABASE_CONFIGURATION = "database";

  public static final String NODE_VERSION = "16.16.0";

  public static final String NPM_INSTALL_TASK_NAME = "npm-install";
  public static final String NPM_BUILD_TASK_NAME = "npm-build";

  @Override
  public void apply(Project project) {
    project.getConfigurations().create(DATABASE_CONFIGURATION);
    applyConfigurationLibs(project, DATABASE_CONFIGURATION, DATABASE_CONFIGURATION);

    project.getPlugins().apply(NodePlugin.class);

    final NodeExtension nodeExtension = NodeExtension.get(project);
    final File webappDir = new File(project.getBuildDir(), "webapp");
    nodeExtension.getNodeProjectDir().set(webappDir);
    nodeExtension.getVersion().set(NODE_VERSION);
    nodeExtension.getDistBaseUrl().set((String) null);
    nodeExtension.getDownload().set(true);

    project
        .getTasks()
        .create(
            NPM_INSTALL_TASK_NAME,
            NpmInstallTask.class,
            task -> {
              task.setDescription("Run 'npm install' command to install npm packages.");
              task.setGroup(AxelorPlugin.AXELOR_BUILD_GROUP);
              task.dependsOn(WarSupport.COPY_WEBAPP_TASK_NAME);
            });

    NpmTask npmTask =
        project
            .getTasks()
            .create(
                NPM_BUILD_TASK_NAME,
                NpmTask.class,
                task -> {
                  task.setDescription("Run 'npm run build' command to build web resource bundles.");
                  task.setGroup(AxelorPlugin.AXELOR_BUILD_GROUP);
                  task.getArgs().set(List.of("run", "build"));
                  task.dependsOn(NPM_INSTALL_TASK_NAME);
                  task.getInputs()
                      .files(project.fileTree(webappDir, tree -> tree.exclude("dist/**")));
                  task.getOutputs().dir(new File(webappDir, "dist"));
                });

    project.afterEvaluate(
        p -> {
          project.getTasks().getByName(WarPlugin.WAR_TASK_NAME).mustRunAfter(npmTask);
        });

    project
        .getTasks()
        .create(
            DatabaseTask.TASK_NAME,
            DatabaseTask.class,
            task -> {
              task.setDescription(DatabaseTask.TASK_DESCRIPTION);
              task.setGroup(DatabaseTask.TASK_GROUP);
            });
  }
}
