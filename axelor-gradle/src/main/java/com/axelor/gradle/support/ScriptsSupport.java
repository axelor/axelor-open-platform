/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.gradle.support;

import com.axelor.gradle.tasks.DatabaseTask;
import org.gradle.api.Project;

public class ScriptsSupport extends AbstractSupport {

  public static final String DATABASE_CONFIGURATION = "database";

  @Override
  public void apply(Project project) {
    project.getConfigurations().create(DATABASE_CONFIGURATION);
    applyConfigurationLibs(project, DATABASE_CONFIGURATION, DATABASE_CONFIGURATION);

    project
        .getTasks()
        .register(
            DatabaseTask.TASK_NAME,
            DatabaseTask.class,
            task -> {
              task.setDescription(DatabaseTask.TASK_DESCRIPTION);
              task.setGroup(DatabaseTask.TASK_GROUP);
            });
  }
}
