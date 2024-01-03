/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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

import com.axelor.gradle.ChangelogExtension;
import com.axelor.gradle.tasks.GenerateChangelog;
import org.gradle.api.Project;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.TaskProvider;

public class ChangelogSupport extends AbstractSupport {

  @Override
  public void apply(Project project) {
    ChangelogExtension changelogExtension =
        project
            .getExtensions()
            .create(ChangelogExtension.EXTENSION_NAME, ChangelogExtension.class, project);

    TaskProvider<GenerateChangelog> generateChangelogTaskProvider =
        project
            .getTasks()
            .register(
                GenerateChangelog.TASK_NAME,
                GenerateChangelog.class,
                task -> {
                  task.setDescription(GenerateChangelog.TASK_DESCRIPTION);
                  task.setGroup(GenerateChangelog.TASK_GROUP);
                  // task is never up-to-date, always run due to --preview option
                  task.getOutputs().upToDateWhen(Specs.satisfyNone());
                });

    project.afterEvaluate(
        unused -> {
          generateChangelogTaskProvider.configure(
              task -> {
                task.setVersion(changelogExtension.getVersion().get());
                task.setChangelogPath(changelogExtension.getOutput().get().getAsFile());
                task.setInputPath(changelogExtension.getInputPath().get().getAsFile());
                task.setTypes(changelogExtension.getTypes().get());
                task.setHeader(changelogExtension.getHeader().get());
              });
        });
  }
}
