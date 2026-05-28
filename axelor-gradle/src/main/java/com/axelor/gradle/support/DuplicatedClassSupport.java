/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.gradle.support;

import com.axelor.gradle.tasks.CheckDuplicateClassesTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;

/** A plugin to check duplicated classes in the classpath. */
public class DuplicatedClassSupport implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project
        .getTasks()
        .register(
            CheckDuplicateClassesTask.TASK_NAME,
            CheckDuplicateClassesTask.class,
            task -> {
              task.setDescription(CheckDuplicateClassesTask.TASK_DESCRIPTION);
              task.setGroup(CheckDuplicateClassesTask.TASK_GROUP);
              task.setClasspathToScan(
                  project
                      .getConfigurations()
                      .getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME));
            });
  }
}
