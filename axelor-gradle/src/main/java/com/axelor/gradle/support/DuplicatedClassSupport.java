/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
