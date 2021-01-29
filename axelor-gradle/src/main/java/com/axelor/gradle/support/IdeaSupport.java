/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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

import com.axelor.common.FileUtils;
import com.axelor.gradle.AppPlugin;
import com.axelor.gradle.AxelorPlugin;
import com.axelor.gradle.tasks.GenerateCode;
import com.axelor.gradle.tasks.TomcatRun;
import com.axelor.tools.ide.IdeaHelper;
import java.io.File;
import java.util.List;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModel;

public class IdeaSupport extends AbstractSupport {

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(IdeaPlugin.class);
    project.afterEvaluate(
        p -> {
          if (project.getPlugins().hasPlugin(AxelorPlugin.class)) {
            project.getTasks().getByName("ideaModule").dependsOn(GenerateCode.TASK_NAME);
            project
                .getTasks()
                .getByName(
                    GenerateCode.TASK_NAME,
                    task -> {
                      project
                          .getExtensions()
                          .getByType(IdeaModel.class)
                          .getModule()
                          .getGeneratedSourceDirs()
                          .addAll(((GenerateCode) task).getOutputDirectories());
                    });
          }
          if (project.getPlugins().hasPlugin(AppPlugin.class)) {
            final File rootDir = project.getRootDir();
            final File workspace = FileUtils.getFile(rootDir, ".idea", "workspace.xml");
            final List<String> args = TomcatRun.getArgs(project, 8080);
            final List<String> vmArgs = TomcatRun.getJvmArgs(project, false);
            project.getTasks().getByName("ideaModule").dependsOn(WarSupport.COPY_WEBAPP_TASK_NAME);
            project
                .getTasks()
                .create(
                    "generateIdeaLauncher",
                    task -> {
                      task.onlyIf(t -> workspace.exists());
                      task.doLast(
                          t -> IdeaHelper.createLauncher(rootDir, project.getName(), args, vmArgs));
                      Task generateLauncher = project.getTasks().getByName("generateLauncher");
                      if (generateLauncher != null) {
                        generateLauncher.finalizedBy(task);
                      }
                    });
          }
        });
  }
}
