/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.gradle.support;

import com.axelor.gradle.tasks.CopyWebapp;
import com.axelor.gradle.tasks.GenerateCode;
import org.gradle.api.Project;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.tasks.bundling.War;

public class WarSupport extends AbstractSupport {

  public static final String COPY_WEBAPP_TASK_NAME = "copyWebapp";

  @Override
  public void apply(Project project) {

    project.getPlugins().apply(WarPlugin.class);

    // apply providedCompile dependencies
    applyConfigurationLibs(project, "provided", "compileOnly");

    // copy webapp to root build dir
    project
        .getTasks()
        .register(
            COPY_WEBAPP_TASK_NAME,
            CopyWebapp.class,
            task -> {
              task.dependsOn(GenerateCode.MAIN_TASK_NAME);
              task.dependsOn(JavaPlugin.PROCESS_RESOURCES_TASK_NAME);
            });

    project.getTasks().withType(War.class).all(task -> task.dependsOn(COPY_WEBAPP_TASK_NAME));

    final War war = (War) project.getTasks().getByName(WarPlugin.WAR_TASK_NAME);
    war.from(project.getLayout().getBuildDirectory().dir("webapp"));
    war.exclude("**/.*");
    war.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
  }
}
