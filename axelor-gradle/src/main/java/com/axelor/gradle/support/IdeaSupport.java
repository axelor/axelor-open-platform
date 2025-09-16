/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.gradle.support;

import com.axelor.gradle.AxelorPlugin;
import com.axelor.gradle.tasks.GenerateCode;
import org.gradle.api.Project;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModel;

public class IdeaSupport extends AbstractSupport {

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(IdeaPlugin.class);
    project.afterEvaluate(
        p -> {
          if (project.getPlugins().hasPlugin(AxelorPlugin.class)) {
            project.getTasks().getByName("ideaModule").dependsOn(GenerateCode.MAIN_TASK_NAME);
            project.getTasks().getByName("ideaModule").dependsOn(WarSupport.COPY_WEBAPP_TASK_NAME);
            project
                .getTasks()
                .getByName(
                    GenerateCode.MAIN_TASK_NAME,
                    task -> {
                      project
                          .getExtensions()
                          .getByType(IdeaModel.class)
                          .getModule()
                          .getGeneratedSourceDirs()
                          .addAll(((GenerateCode) task).getOutputDirectories());
                    });
          }
        });
  }
}
