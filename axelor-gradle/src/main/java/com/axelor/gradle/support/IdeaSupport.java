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
            project.getTasks().getByName("ideaModule").dependsOn(GenerateCode.TASK_NAME);
            project.getTasks().getByName("ideaModule").dependsOn(WarSupport.COPY_WEBAPP_TASK_NAME);
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
        });
  }
}
