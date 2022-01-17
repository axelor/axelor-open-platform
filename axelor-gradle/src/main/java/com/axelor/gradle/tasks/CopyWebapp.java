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
package com.axelor.gradle.tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.util.PatternSet;

public class CopyWebapp extends ModuleTask {

  private PatternSet pattern = new PatternSet().include("webapp/**/*");

  @OutputDirectory
  public File getOutputDir() {
    return new File(getProject().getBuildDir(), "webapp");
  }

  @InputFiles
  public List<FileTree> getFiles() {
    List<FileTree> files = new ArrayList<>();

    // first include module webapp resources
    moduleArtifacts().stream().map(this::webapp).forEach(files::add);

    // than include own webapp resources
    files.add(getProject().fileTree("src/main").matching(pattern));

    return files;
  }

  private FileTree webapp(ResolvedArtifact artifact) {
    Project module = findProject(artifact);
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
          task.into(project.getBuildDir());
          task.from(getFiles());
        });
  }
}
