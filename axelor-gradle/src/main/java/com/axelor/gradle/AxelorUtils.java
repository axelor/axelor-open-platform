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
package com.axelor.gradle;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.internal.composite.IncludedBuildInternal;

public class AxelorUtils {

  private AxelorUtils() {}

  private static LoadingCache<Project, List<Project>> includedBuildRootsCache =
      CacheBuilder.newBuilder()
          .build(
              new CacheLoader<Project, List<Project>>() {
                @Override
                public List<Project> load(Project project) throws Exception {
                  return project.getGradle().getIncludedBuilds().stream()
                      .map(b -> ((IncludedBuildInternal) b).getTarget())
                      .map(b -> b.getMutableModel().getRootProject())
                      .collect(Collectors.toList());
                }
              });

  public static String toRelativePath(Project project, File file) {
    return project.getProjectDir().toPath().relativize(file.toPath()).toString();
  }

  private static List<Project> includedBuildRoots(Project project) {
    return includedBuildRootsCache.getUnchecked(project);
  }

  public static List<Project> findIncludedBuildProjects(Project project) {
    return includedBuildRoots(project).stream()
        .flatMap(root -> Stream.concat(Stream.of(root), root.getSubprojects().stream()))
        .collect(Collectors.toList());
  }

  public static List<Project> findAxelorProjects(Project project) {
    return findAxelorArtifacts(project).stream()
        .map(artifact -> findProject(project, artifact))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public static List<ResolvedArtifact> findAxelorArtifacts(Project project) {
    final Configuration config = project.getConfigurations().getByName("runtimeClasspath");
    final Set<Object> visited = new LinkedHashSet<>();
    final List<ResolvedArtifact> result = new ArrayList<>();
    config
        .getResolvedConfiguration()
        .getFirstLevelModuleDependencies()
        .forEach(it -> sortArtifacts(project, it, visited, result));
    return Collections.unmodifiableList(result);
  }

  private static void sortArtifacts(
      Project project,
      ResolvedDependency dependency,
      Set<Object> visited,
      List<ResolvedArtifact> result) {
    if (visited.contains(dependency.getName())) {
      return;
    }
    visited.add(dependency.getName());
    final Set<ResolvedArtifact> artifacts = dependency.getModuleArtifacts();
    for (ResolvedArtifact artifact : artifacts) {
      if (isAxelorModule(project, artifact)) {
        for (ResolvedDependency child : dependency.getChildren()) {
          sortArtifacts(project, child, visited, result);
        }
        result.add(artifact);
      }
    }
  }

  public static boolean isAxelorModule(Project project, ResolvedArtifact artifact) {
    final Project sub = findProject(project, artifact);
    if (sub == null) {
      try (JarFile jar = new JarFile(artifact.getFile())) {
        if (jar.getEntry("META-INF/axelor-module.properties") != null) {
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

  private static final Object LOCK = new Object();

  public static String getModuleName(Project project, ResolvedArtifact artifact)
      throws IOException {

    // Try by project
    Project sub = findProject(project, artifact);
    if (sub != null) {
      return sub.getName();
    }

    // Try in jar

    synchronized (LOCK) {
      try (final URLClassLoader loader =
              new URLClassLoader(new URL[] {artifact.getFile().toURI().toURL()});
          final InputStream in = loader.getResourceAsStream("META-INF/axelor-module.properties")) {
        if (in != null) {
          Properties props = new java.util.Properties();
          props.load(in);
          return (String) props.get("name");
        } else {
          throw new IOException(
              "Unable to locate axelor-module.properties in " + artifact.getName());
        }
      }
    }
  }

  public static Project findProject(Project project, ResolvedArtifact artifact) {
    final ComponentIdentifier cid = artifact.getId().getComponentIdentifier();
    if (cid instanceof ProjectComponentIdentifier) {
      String path = ((ProjectComponentIdentifier) cid).getProjectPath();
      Project sub = project.findProject(path);
      // consider projects from included builds
      if (sub == null) {
        sub =
            includedBuildRoots(project).stream()
                .map(p -> p.findProject(path))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
      }
      return sub;
    }
    return null;
  }

  public static boolean isCore(Project project) {
    String name = project.getName();
    return "axelor-core".equals(name) || "axelor-web".equals(name) || "axelor-test".equals(name);
  }

  public static boolean isCore(String name) {
    return "axelor-core".equals(name) || "axelor-web".equals(name) || "axelor-test".equals(name);
  }

  public static boolean isAxelorApplication(Project project) {
    return project == project.getRootProject()
        && (!project.hasProperty("axelor.application")
            || Boolean.parseBoolean((String) project.getProperties().get("axelor.application")));
  }
}
