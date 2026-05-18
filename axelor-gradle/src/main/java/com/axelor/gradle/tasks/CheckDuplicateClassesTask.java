/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.gradle.tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationTask;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

public class CheckDuplicateClassesTask extends DefaultTask implements VerificationTask {

  public static final String TASK_NAME = "checkDuplicateClasses";

  public static final String TASK_DESCRIPTION = "Check for conflicting dependencies in the project";

  public static final String TASK_GROUP = LifecycleBasePlugin.VERIFICATION_GROUP;

  private boolean ignoreFailures = false;

  private Set<String> excludes = new HashSet<>();

  private FileCollection classpathToScan;

  @Classpath
  public FileCollection getClasspathToScan() {
    return classpathToScan;
  }

  public void setClasspathToScan(FileCollection classpathToScan) {
    this.classpathToScan = classpathToScan;
  }

  public void setExcludes(Set<String> excludes) {
    this.excludes = excludes;
  }

  @Input
  public Set<String> getExcludes() {
    return excludes;
  }

  private record JarScan(boolean axelorModule, List<String> classNames) {}

  private boolean isExcluded(File file) {
    String name = file.getName();
    return excludes.stream().anyMatch(name::matches);
  }

  private JarScan scanJar(File file) {
    try (var jar = new JarFile(file)) {
      // O(1) lookup — Axelor jars short-circuit without enumerating entries.
      if (jar.getJarEntry("META-INF/axelor-module.properties") != null) {
        return new JarScan(true, List.of());
      }
      var names = new ArrayList<String>();
      var entries = jar.entries();
      while (entries.hasMoreElements()) {
        var name = entries.nextElement().getName();
        if (name.endsWith(".class") && !name.endsWith("module-info.class")) {
          names.add(name.substring(0, name.length() - ".class".length()).replace('/', '.'));
        }
      }
      return new JarScan(false, names);
    } catch (IOException e) {
      throw new GradleException("Error reading jar: " + file, e);
    }
  }

  @TaskAction
  public void check() {
    Map<String, List<String>> libs =
        classpathToScan.getFiles().parallelStream()
            .filter(f -> f.isFile() && f.getName().endsWith(".jar"))
            .filter(f -> !isExcluded(f))
            .map(f -> Map.entry(f.getName(), scanJar(f)))
            .filter(e -> !e.getValue().axelorModule())
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().classNames()));

    // class name -> jars that declare it.
    Map<String, List<String>> classOwners = new HashMap<>();
    for (var entry : libs.entrySet()) {
      var lib = entry.getKey();
      for (var cls : entry.getValue()) {
        classOwners.computeIfAbsent(cls, k -> new ArrayList<>(2)).add(lib);
      }
    }

    // Canonical (A < B) pairs — deterministic and no mirror dedup needed.
    Map<String, Set<String>> conflicts = new TreeMap<>();
    for (var owners : classOwners.values()) {
      if (owners.size() < 2) {
        continue;
      }
      for (int i = 0; i < owners.size(); i++) {
        for (int j = i + 1; j < owners.size(); j++) {
          var a = owners.get(i);
          var b = owners.get(j);
          if (a.equals(b)) {
            continue;
          }
          if (a.compareTo(b) > 0) {
            var t = a;
            a = b;
            b = t;
          }
          conflicts.computeIfAbsent(a, k -> new TreeSet<>()).add(b);
        }
      }
    }

    if (conflicts.isEmpty()) {
      getLogger().info("No conflicts found.");
      return;
    }

    // Print conflicts
    getLogger().lifecycle("Conflicts found:");
    getLogger().lifecycle("");

    conflicts.forEach(
        (lib, others) -> {
          getLogger().lifecycle("Library: " + lib);
          getLogger().lifecycle("Conflicts with: " + String.join(", ", others));
          getLogger().lifecycle("");
        });

    if (!ignoreFailures) {
      throw new GradleException("Duplicate classes detected");
    }
  }

  @Override
  public void setIgnoreFailures(boolean ignoreFailures) {
    this.ignoreFailures = ignoreFailures;
  }

  @Override
  public boolean getIgnoreFailures() {
    return ignoreFailures;
  }
}
