/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
                task.setAllowNoEntry(changelogExtension.getAllowNoEntry().get());
                task.setDefaultContent(changelogExtension.getDefaultContent().get());
              });
        });
  }
}
