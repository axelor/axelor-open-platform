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
package com.axelor.gradle;

import com.axelor.common.StringUtils;
import com.axelor.common.VersionUtils;
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
import org.gradle.api.artifacts.DependencySubstitution;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.internal.tasks.JvmConstants;
import org.gradle.internal.composite.IncludedBuildInternal;

public class AxelorUtils {

  private static final Set<String> CORE_MODULES = Set.of("axelor-core", "axelor-web");

  private static final Set<String> TEST_MODULES = Set.of("axelor-test");

  private static final Set<String> ENTERPRISE_MODULES = Set.of("axelor-enterprise");

  private static final String EE_SUFFIX = "enterprise";
  private static final String USE_EE_PLATFORM_PROPERTY = "axelor.platform.ee";

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

  private static String getModuleProperty(Project project, ResolvedArtifact artifact, String name)
      throws IOException {
    synchronized (LOCK) {
      try (final URLClassLoader loader =
              new URLClassLoader(new URL[] {artifact.getFile().toURI().toURL()});
          final InputStream in = loader.getResourceAsStream("META-INF/axelor-module.properties")) {
        if (in != null) {
          Properties props = new java.util.Properties();
          props.load(in);
          return props.getProperty(name);
        } else {
          throw new IOException(
              "Unable to locate axelor-module.properties in " + artifact.getName());
        }
      }
    }
  }

  public static String getModuleName(Project project, ResolvedArtifact artifact)
      throws IOException {
    // Try by project
    Project sub = findProject(project, artifact);
    if (sub != null) {
      return sub.getName();
    }
    // Try in jar
    return getModuleProperty(project, artifact, "name");
  }

  public static String getGroupName(Project project, ResolvedArtifact artifact) throws IOException {
    // Try by project
    Project sub = findProject(project, artifact);
    if (sub != null) {
      return (String) sub.getGroup();
    }
    // Try in jar
    String group = getModuleProperty(project, artifact, "group");
    if (StringUtils.notBlank(group)) {
      return group;
    }
    // Try from maven artifact
    ComponentIdentifier identifier = artifact.getId().getComponentIdentifier();
    if (identifier instanceof ModuleComponentIdentifier) {
      return ((ModuleComponentIdentifier) identifier).getGroup();
    }
    throw new IOException("Unable to find module group id for " + artifact.getName());
  }

  public static Project findProject(Project project, ResolvedArtifact artifact) {
    final ComponentIdentifier cid = artifact.getId().getComponentIdentifier();
    if (cid instanceof ProjectComponentIdentifier) {
      ProjectComponentIdentifier id = (ProjectComponentIdentifier) cid;
      String path = id.getProjectPath();
      Project sub = project.findProject(path);
      if (":".equals(path)) {
        sub =
            includedBuildRoots(project).stream()
                .filter(p -> p.getName().equals(id.getProjectName()))
                .findFirst()
                .orElse(sub);
      }
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

  private static Project findProject(Project project, String name) {
    final Project root = project.getRootProject();
    final Project match = root.findProject(":" + name);
    if (match != null) {
      return match;
    }
    return findIncludedBuildProjects(project).stream()
        .filter(x -> x.getName().equals(name))
        .findFirst()
        .orElse(null);
  }

  static void addDependencies(Project project) {
    // add implementation dependencies
    addImplementations(project);
    // add testImplementation dependencies
    addTestImplementations(project);
    // add substitutions
    addSubstitutions(project);
  }

  private static void addSubstitutions(Project project) {
    final String core = CORE_MODULES.iterator().next();
    final Object coreProject = findProject(project, core);
    if (coreProject != null) {
      return;
    }

    if (shouldUsePlatformEE(project)) {
      project
          .getConfigurations()
          .all(
              config ->
                  config
                      .getResolutionStrategy()
                      .getDependencySubstitution()
                      .all(AxelorUtils::addEESubstitutions));
    }
  }

  private static void addEESubstitutions(DependencySubstitution substitution) {
    if (substitution.getRequested() instanceof ModuleComponentSelector) {
      ModuleComponentSelector selector = (ModuleComponentSelector) substitution.getRequested();
      String group = selector.getGroup();
      String name = selector.getModule();
      String version = selector.getVersion();
      if ("com.axelor".equals(group) && (CORE_MODULES.contains(name))) {
        String target = toDependency(name, version, true);
        substitution.useTarget(target);
      }
    }
  }

  private static void addImplementations(Project project) {
    final boolean useEE = shouldUsePlatformEE(project);
    final String version = VersionUtils.getVersion().version;
    final String config = JvmConstants.IMPLEMENTATION_CONFIGURATION_NAME;

    // add core modules
    CORE_MODULES.forEach(module -> addDependency(project, config, module, version, useEE));

    // add enterprise modules
    if (useEE) {
      ENTERPRISE_MODULES.forEach(module -> addDependency(project, config, module, version, false));
    }
  }

  private static void addTestImplementations(Project project) {
    final String version = VersionUtils.getVersion().version;
    final String config = JvmConstants.TEST_IMPLEMENTATION_CONFIGURATION_NAME;
    TEST_MODULES.forEach(module -> addDependency(project, config, module, version, false));
  }

  private static void addDependency(
      Project project, String configuration, String module, String version, boolean useEE) {
    // find a project
    Object dependency = findProject(project, module);
    if (dependency == null) {
      // otherwise use jar
      dependency = toDependency(module, version, useEE);
    }
    // add dependency
    project.getDependencies().add(configuration, dependency);
  }

  private static String toDependency(String module, String version, boolean useEE) {
    String name = findVariantName(module, useEE);
    return String.format("com.axelor:%s:%s", name, version);
  }

  public static String findVariantName(String module, boolean useEE) {
    // Only core modules have EE
    if (useEE && CORE_MODULES.contains(module)) {
      return String.format("%s-%s", module, EE_SUFFIX);
    }
    return module;
  }

  private static boolean shouldUsePlatformEE(Project project) {
    return Boolean.parseBoolean((String) project.findProperty(USE_EE_PLATFORM_PROPERTY));
  }

  public static boolean isCore(Project project) {
    return isCore(project.getName());
  }

  public static boolean isCore(String name) {
    return CORE_MODULES.contains(name)
        || TEST_MODULES.contains(name)
        || ENTERPRISE_MODULES.contains(name);
  }

  public static boolean isAxelorApplication(Project project) {
    return project == project.getRootProject()
        && (!project.hasProperty("axelor.application")
            || Boolean.parseBoolean((String) project.getProperties().get("axelor.application")));
  }
}
