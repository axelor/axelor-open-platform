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
package com.axelor.gradle.tasks;

import com.axelor.common.reflections.Reflections;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  private URL toURL(File file) {
    try {
      return file.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new RuntimeException("Invalid file URL: " + file, e);
    }
  }

  private List<URL> findWithin(URL jar, String pattern) {
    URL[] urls = {jar};
    try (var loader = new URLClassLoader(urls, null)) {
      return Reflections.findResources(loader).byName(pattern).find().stream()
          .filter(x -> !x.toString().contains("module-info"))
          .toList();
    } catch (Exception e) {
      throw new RuntimeException("Error finding resources in jar: " + jar, e);
    }
  }

  private List<URL> findClassURLs(URL jar) {
    return findWithin(jar, ".*\\.class");
  }

  private boolean isNotAxelorLib(URL jar) {
    return findWithin(jar, "META-INF/axelor-module.properties").isEmpty();
  }

  private boolean isExcluded(URL jar) {
    String jarName = new File(jar.getFile()).getName();
    return excludes.stream().anyMatch(jarName::matches);
  }

  private boolean isCandidate(URL jar) {
    return isNotAxelorLib(jar) && !isExcluded(jar);
  }

  private List<String> findClassNames(URL jar) {
    return findClassURLs(jar).stream()
        .map(
            url -> {
              return url.getPath()
                  .replaceAll(".*\\.jar!", "")
                  .replaceAll("^/", "")
                  .replaceAll("\\.class$", "")
                  .replaceAll("/", ".");
            })
        .toList();
  }

  @TaskAction
  public void check() {
    var urls = classpathToScan.getFiles().stream().map(this::toURL).toList();
    var libs =
        urls.parallelStream()
            .filter(this::isCandidate)
            .map(
                url -> {
                  var name = new File(url.getFile()).getName();
                  var classes = findClassNames(url);
                  return Map.entry(name, classes);
                })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    var conflicts = new HashMap<String, List<String>>();

    // Find conflicts between libraries
    for (var entry : libs.entrySet()) {
      var lib = entry.getKey();
      var classes = entry.getValue();
      for (var otherEntry : libs.entrySet()) {
        var otherLib = otherEntry.getKey();
        if (lib.equals(otherLib)) {
          continue;
        }
        var otherClasses = otherEntry.getValue();
        if (otherClasses.stream().anyMatch(classes::contains)
            // Don't add mirrored duplicates (B,A) if (A,B) exist
            && (conflicts.get(otherLib) == null || !conflicts.get(otherLib).contains(lib))) {
          conflicts.computeIfAbsent(lib, k -> new ArrayList<>()).add(otherLib);
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

    for (var entry : conflicts.entrySet()) {
      var lib = entry.getKey();
      var conflictingLibs = entry.getValue();
      getLogger().lifecycle("Library: " + lib);
      getLogger().lifecycle("Conflicts with: " + String.join(", ", conflictingLibs));
      getLogger().lifecycle("");
    }

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
