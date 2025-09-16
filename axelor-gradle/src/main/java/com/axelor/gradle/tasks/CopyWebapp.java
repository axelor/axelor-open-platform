/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.gradle.tasks;

import com.axelor.gradle.AxelorUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.util.PatternSet;

public class CopyWebapp extends DefaultTask {

  private PatternSet pattern = new PatternSet().include("webapp/**/*");

  @OutputDirectory
  public File getOutputDir() {
    return new File(getProject().getLayout().getBuildDirectory().get().getAsFile(), "webapp");
  }

  @InputFiles
  public List<FileTree> getFrontFiles() {
    List<FileTree> files = new ArrayList<>();
    AxelorUtils.findAxelorArtifacts(getProject()).stream()
        .filter(it -> "axelor-web".equals(it.getName()))
        .forEach(
            artifact -> {
              Project module = AxelorUtils.findProject(getProject(), artifact);
              if (module != null) {
                files.add(module.fileTree("../axelor-front/dist"));
              }
            });
    return files;
  }

  @InputFiles
  public List<FileTree> getFiles() {
    List<FileTree> files = new ArrayList<>();

    // first include module webapp resources
    AxelorUtils.findAxelorArtifacts(getProject()).stream().map(this::webapp).forEach(files::add);

    // than include own webapp resources
    files.add(getProject().fileTree("src/main").matching(pattern));

    return files;
  }

  private FileTree webapp(ResolvedArtifact artifact) {
    Project module = AxelorUtils.findProject(getProject(), artifact);
    return module == null
        ? getProject().zipTree(artifact.getFile()).matching(pattern)
        : module.fileTree("src/main").matching(pattern);
  }

  @TaskAction
  public void copy() throws IOException {
    Project project = getProject();
    project.copy(
        task -> {
          task.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE);
          task.into(project.getLayout().getBuildDirectory());
          task.from(getFiles());
          task.into(
              project.getLayout().getBuildDirectory().dir("/webapp").get().getAsFile(),
              copySpec -> {
                copySpec.from(getFrontFiles());
                copySpec.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
              });
        });
  }
}
