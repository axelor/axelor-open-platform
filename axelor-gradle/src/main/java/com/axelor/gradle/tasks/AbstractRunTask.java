/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.gradle.process.ExecOperations;
import org.gradle.process.JavaExecSpec;

public abstract class AbstractRunTask extends DefaultTask {

  private ExecOperations execOperations;
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
            .getExtensions()
            .getByType(JavaPluginExtension.class)
            .getSourceSets()
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            .getRuntimeClasspath();
    return classpath;
  }

  @Internal
  protected File getManifestJar() {
    return FileUtils.getFile(
        getProject().getLayout().getBuildDirectory().get().getAsFile(), "classpath.jar");
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

  public AbstractRunTask(ExecOperations execOperations) {
    this.execOperations = execOperations;
  }

  @TaskAction
  public void exec() throws Exception {
    execOperations.javaexec(
        task -> {
          task.classpath(createManifestJar());
          task.args(getArgs());
          task.jvmArgs(getJvmArgs());
          configure(task);
        });
  }
}
