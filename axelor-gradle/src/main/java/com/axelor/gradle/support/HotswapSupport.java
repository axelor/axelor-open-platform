/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
package com.axelor.gradle.support;

import com.axelor.common.FileUtils;
import com.axelor.gradle.HotswapExtension;
import com.google.common.base.Joiner;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.composite.internal.DefaultIncludedBuild;

public class HotswapSupport extends AbstractSupport {

  public static final String GENERATE_HOTSWAP_CONFIG_TASK = "generateHotswapConfig";

  private static boolean hasDCEVM;

  static {
    final Path home = Paths.get(System.getProperty("java.home"));
    final List<String> paths =
        Arrays.asList("lib/amd64/dcevm", "lib/i386/dcevm", "lib/dcevm", "bin/dcevm");
    final List<String> libs = Arrays.asList("libjvm.so", "libjvm.dylib", "jvm.dll");
    for (String path : paths) {
      for (String name : libs) {
        if (Files.exists(home.resolve(Paths.get(path, name)))) {
          hasDCEVM = true;
        }
      }
    }
  }

  public static boolean hasDCEVM() {
    return hasDCEVM;
  }

  public static List<String> getAgentArgs(Project project, boolean auto) {
    if (!hasDCEVM()) {
      return Collections.emptyList();
    }
    final Configuration tomcat =
        project.getConfigurations().getByName(TomcatSupport.TOMCAT_CONFIGURATION);
    final List<String> args = new ArrayList<>();
    tomcat.getFiles().stream()
        .filter(f -> f.getName().endsWith(".jar"))
        .filter(f -> f.getName().startsWith("hotswap-agent-core"))
        .findFirst()
        .ifPresent(
            agentJar -> {
              final File config = new File(project.getBuildDir(), "hotswap-agent.properties");
              final StringBuilder agent =
                  new StringBuilder("-javaagent:").append(agentJar.getAbsolutePath());
              final List<String> agentArgs = new ArrayList<>();
              if (config.exists()) {
                agentArgs.add("propertiesFilePath=" + toRelativePath(project, config));
              }
              if (auto) {
                agentArgs.add("autoHotswap=true");
              }
              if (agentArgs.size() > 0) {
                agent.append("=").append(Joiner.on(",").skipNulls().join(agentArgs));
              }
              args.add("-XXaltjvm=dcevm");
              args.add(agent.toString());
            });
    return args;
  }

  @Override
  public void apply(Project project) {
    project.getExtensions().create(HotswapExtension.EXTENSION_NAME, HotswapExtension.class);
    project
        .getTasks()
        .create(
            GENERATE_HOTSWAP_CONFIG_TASK,
            task -> {
              task.setDescription("Generate hotswap-agent.properties");
              task.onlyIf(t -> hasDCEVM());
              task.doLast(t -> generateHotswapConfig(project));
            });
  }

  private static List<File> findProjectDirs(Project project, Function<File, Stream<File>> mapper) {
    final Set<File> path =
        project
            .getConvention()
            .getPlugin(JavaPluginConvention.class)
            .getSourceSets()
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            .getRuntimeClasspath()
            .getFiles();

    return Stream.concat(
            project.getAllprojects().stream(),
            project.getGradle().getIncludedBuilds().stream()
                .map(DefaultIncludedBuild.class::cast)
                .flatMap(b -> b.getConfiguredBuild().getRootProject().getAllprojects().stream()))
        .filter(p -> !p.getName().equals("axelor-gradle"))
        .filter(p -> !p.getName().equals("axelor-tomcat"))
        .filter(p -> !p.getName().equals("axelor-test"))
        .filter(p -> FileUtils.getFile(p.getProjectDir(), "build.gradle").exists())
        .filter(p -> path.stream().anyMatch(f -> f.toPath().startsWith(p.getProjectDir().toPath())))
        .map(Project::getProjectDir)
        .flatMap(mapper)
        .filter(File::exists)
        .collect(Collectors.toList());
  }

  private static Stream<File> findResourceDirs(File projectDir) {
    return Stream.of(FileUtils.getFile(projectDir, "src", "main", "resources"));
  }

  private static Stream<File> findOurputDirs(File projectDir) {
    // eclipse
    if (FileUtils.getFile(projectDir, "bin", "main").exists()) {
      return Stream.of(FileUtils.getFile(projectDir, "bin", "main"));
    }

    // idea
    if (FileUtils.getFile(projectDir, "out", "production").exists()) {
      return Stream.of(
          FileUtils.getFile(projectDir, "out", "production", "classes"),
          FileUtils.getFile(projectDir, "out", "production", "resources"));
    }

    // gradle
    return Stream.of(
        FileUtils.getFile(projectDir, "build", "classes", "java", "main"),
        FileUtils.getFile(projectDir, "build", "resources", "main"));
  }

  /** Find IDE output paths. */
  public static List<File> findOutputPaths(Project project) {
    return findProjectDirs(project, HotswapSupport::findOurputDirs);
  }

  /** Find src/main/resources paths from all projects */
  private static List<File> findResourcePaths(Project project) {
    return findProjectDirs(project, HotswapSupport::findResourceDirs);
  }

  private void generateHotswapConfig(Project project) {
    final Properties hotswapProps = new Properties();
    final HotswapExtension extension = project.getExtensions().getByType(HotswapExtension.class);

    final List<File> extraClasspath = new ArrayList<>();
    final List<File> watchResources = new ArrayList<>();

    if (!extension.getDisabledPlugins().isEmpty()) {
      hotswapProps.setProperty(
          "disabledPlugins", Joiner.on(",").join(extension.getDisabledPlugins()));
    }

    hotswapProps.setProperty("LOGGER", "reload");
    extension.getLoggers().forEach((logger, level) -> hotswapProps.setProperty(logger, level));

    if (extension.getLogFile() != null) {
      hotswapProps.setProperty("logFile", extension.getLogFile().getAbsolutePath());
    }
    if (extension.getLogAppend() != null) {
      hotswapProps.setProperty(
          "logAppend", extension.getLogAppend() == Boolean.TRUE ? "true" : "false");
    }

    if (extension.getExtraClasspath() != null) {
      extension.getExtraClasspath().stream().forEach(extraClasspath::add);
    }

    findOutputPaths(project).forEach(extraClasspath::add);
    findResourcePaths(project).forEach(watchResources::add);

    hotswapProps.setProperty(
        "extraClasspath",
        extraClasspath.stream()
            .filter(File::exists)
            .map(File::getPath)
            .collect(Collectors.joining(",")));

    if (extension.getWatchResources() != null) {
      extension.getWatchResources().stream()
          .filter(File::exists)
          .filter(f -> !watchResources.contains(f))
          .forEach(watchResources::add);
    }

    if (!watchResources.isEmpty()) {
      hotswapProps.setProperty(
          "watchResources",
          watchResources.stream().map(File::getPath).collect(Collectors.joining(",")));
    }

    final File target = new File(project.getBuildDir(), "hotswap-agent.properties");

    // make sure to have parent dir
    target.getParentFile().mkdirs();

    try (OutputStream os = new FileOutputStream(target)) {
      hotswapProps.store(os, null);
    } catch (IOException e) {
      project.getLogger().error(e.getMessage(), e);
    }
  }
}
