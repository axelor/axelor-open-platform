/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.gradle.support;

import com.axelor.common.ResourceUtils;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;

public abstract class AbstractSupport implements Plugin<Project> {

  protected void applyConfigurationLibs(Project project, String libs, String as) {
    final String path = "com/axelor/gradle/%s-libs.txt".formatted(libs);
    try (Reader reader = new InputStreamReader(ResourceUtils.getResourceStream(path))) {
      final DependencyHandler handler = project.getDependencies();
      CharStreams.readLines(reader).forEach(lib -> handler.add(as, lib));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
