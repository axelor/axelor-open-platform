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

import com.axelor.gradle.AxelorPlugin;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.composite.internal.DefaultIncludedBuild;

public abstract class ModuleTask extends DefaultTask {

  private List<ResolvedArtifact> artifacts;

  protected List<ResolvedArtifact> moduleArtifacts() {
    if (artifacts == null) {
      artifacts = moduleArtifacts(getProject().getConfigurations().getByName("runtimeClasspath"));
      artifacts = Collections.unmodifiableList(artifacts);
    }
    return artifacts;
  }

  private List<ResolvedArtifact> moduleArtifacts(Configuration config) {
    final Set<Object> visited = new LinkedHashSet<>();
    final List<ResolvedArtifact> result = new ArrayList<>();
    config
        .getResolvedConfiguration()
        .getFirstLevelModuleDependencies()
        .forEach(it -> sortArtifacts(it, visited, result));
    return result;
  }

  private void sortArtifacts(
      ResolvedDependency dependency, Set<Object> visited, List<ResolvedArtifact> result) {
    if (visited.contains(dependency.getName())) {
      return;
    }
    visited.add(dependency.getName());
    final Set<ResolvedArtifact> artifacts = dependency.getModuleArtifacts();
    for (ResolvedArtifact artifact : artifacts) {
      if (isAxelorModule(artifact)) {
        for (ResolvedDependency child : dependency.getChildren()) {
          sortArtifacts(child, visited, result);
        }
        result.add(artifact);
      }
    }
  }

  private boolean isAxelorModule(ResolvedArtifact artifact) {
    final Project sub = findProject(artifact);
    if (sub == null) {
      try (JarFile jar = new JarFile(artifact.getFile())) {
        if (jar.getEntry("module.properties") != null) {
          return true;
        }
      } catch (IOException e) {
      }
      return false;
    }

    // projects from included build has AxelorPlugin.class loaded with another class loader
    return sub.getPlugins().stream()
        .filter(p -> p.getClass().getName().equals(AxelorPlugin.class.getName()))
        .findFirst()
        .isPresent();
  }

  protected Project findProject(ResolvedArtifact artifact) {
    final ComponentIdentifier cid = artifact.getId().getComponentIdentifier();
    if (cid instanceof ProjectComponentIdentifier) {
      String path = ((ProjectComponentIdentifier) cid).getProjectPath();
      Project sub = getProject().findProject(path);
      // consider projects from included builds
      if (sub == null) {
        sub =
            getProject().getGradle().getIncludedBuilds().stream()
                .map(b -> ((DefaultIncludedBuild) b).getConfiguredBuild().getRootProject())
                .map(p -> p.findProject(path))
                .findFirst()
                .orElse(null);
      }
      return sub;
    }
    return null;
  }
}
