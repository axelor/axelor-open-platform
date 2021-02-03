/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2021 Axelor (<http://axelor.com>).
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
package com.axelor.gradle.tasks;

import com.axelor.common.FileUtils;
import com.axelor.common.StringUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.gradle.process.JavaExecSpec;

public abstract class AbstractRunTask extends DefaultTask {

  private String config;

  @Input
  @Optional
  public String getConfig() {
    return config;
  }

  @Option(option = "config", description = "specify the appliction config file path.")
  public void setConfig(String config) {
    this.config = config;
  }

  @Input
  @Optional
  protected List<String> getJvmArgs() {
    final List<String> jvmArgs = new ArrayList<>();
    if (StringUtils.notBlank(config)) {
      jvmArgs.add("-Daxelor.config=" + config);
    }
    return jvmArgs;
  }

  @Input
  @Optional
  protected List<String> getArgs() {
    final List<String> args = new ArrayList<>();
    return args;
  }

  @Classpath
  protected FileCollection getClasspath() {
    final FileCollection classpath =
        getProject()
            .getConvention()
            .getPlugin(JavaPluginConvention.class)
            .getSourceSets()
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            .getRuntimeClasspath();
    return classpath;
  }

  @InputFile
  protected File getManifestJar() {
    return FileUtils.getFile(getProject().getBuildDir(), "classpath.jar");
  }

  @Input
  protected abstract String getMainClass();

  protected File createManifestJar() {
    final File manifestJar = getManifestJar();
    final Manifest manifest = new Manifest();
    final Attributes attributes = manifest.getMainAttributes();

    try {
      Files.createDirectories(manifestJar.toPath().getParent());
    } catch (IOException e) {
      throw new GradleException("Unexpected error occured.", e);
    }

    attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
    attributes.put(Attributes.Name.MAIN_CLASS, getMainClass());
    attributes.put(
        Attributes.Name.CLASS_PATH,
        getClasspath().getFiles().stream()
            .map(File::toURI)
            .map(Object::toString)
            .collect(Collectors.joining(" ")));

    try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(manifestJar), manifest)) {
      return manifestJar;
    } catch (IOException e) {
      throw new GradleException("Unexpected error occured.", e);
    }
  }

  protected void configure(JavaExecSpec task) {}

  @TaskAction
  public void exec() throws Exception {
    final Project project = getProject();
    project.javaexec(
        task -> {
          task.classpath(createManifestJar());
          task.args(getArgs());
          task.jvmArgs(getJvmArgs());
          configure(task);
        });
  }
}
