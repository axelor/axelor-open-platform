/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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

import com.google.common.collect.Lists;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

public class ChangelogExtension {

  public static final String EXTENSION_NAME = "changelog";
  private static final String DEFAULT_CHANGELOG_FILE = "CHANGELOG.md";
  private static final String DEFAULT_INPUT_PATH = "changelogs/unreleased";

  private Property<String> version;
  private DirectoryProperty inputPath;

  private RegularFileProperty output;

  private ListProperty<String> types;

  private Property<String> header;

  public ChangelogExtension(Project project) {
    this.version = project.getObjects().property(String.class);
    this.version.convention(project.provider(() -> project.getVersion().toString()));

    this.types = project.getObjects().listProperty(String.class);
    this.types.convention(
        Lists.newArrayList("Feature", "Change", "Deprecate", "Remove", "Fix", "Security"));

    this.header = project.getObjects().property(String.class);
    this.header.convention(
        project.provider(
            () ->
                String.format(
                    "%s (%s)",
                    project.getVersion().toString(),
                    new SimpleDateFormat("yyyy-MM-dd").format(new Date()))));

    this.output = project.getObjects().fileProperty();
    this.output.convention(
        project
            .getObjects()
            .fileProperty()
            .convention(project.getLayout().getProjectDirectory().file(DEFAULT_CHANGELOG_FILE)));

    this.inputPath = project.getObjects().directoryProperty();
    this.inputPath.convention(
        project
            .getObjects()
            .directoryProperty()
            .convention(project.getLayout().getProjectDirectory().dir(DEFAULT_INPUT_PATH)));
  }

  public Property<String> getVersion() {
    return version;
  }

  public void setVersion(Property<String> version) {
    this.version = version;
  }

  public DirectoryProperty getInputPath() {
    return inputPath;
  }

  public void setInputPath(DirectoryProperty inputPath) {
    this.inputPath = inputPath;
  }

  public RegularFileProperty getOutput() {
    return output;
  }

  public void setOutput(RegularFileProperty output) {
    this.output = output;
  }

  public ListProperty<String> getTypes() {
    return types;
  }

  public void setTypes(ListProperty<String> types) {
    this.types = types;
  }

  public Property<String> getHeader() {
    return header;
  }

  public void setHeader(Property<String> header) {
    this.header = header;
  }
}
