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
package com.axelor.gradle.tasks;

import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.gradle.AxelorPlugin;
import com.axelor.tools.changelog.ChangelogEntry;
import com.axelor.tools.changelog.ChangelogEntryParser;
import com.axelor.tools.changelog.Release;
import com.axelor.tools.changelog.ReleaseGenerator;
import com.axelor.tools.changelog.ReleaseProcessor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

public class GenerateChangelog extends DefaultTask {

  public static final String TASK_DESCRIPTION = "Generate changelog from unreleased entries.";

  public static final String TASK_GROUP = AxelorPlugin.AXELOR_APP_GROUP;

  public static final String TASK_NAME = "generateChangelog";

  @OutputFile private File changelogPath;

  public File getChangelogPath() {
    return changelogPath;
  }

  public void setChangelogPath(File changelogPath) {
    this.changelogPath = changelogPath;
  }

  @Input private String version;

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @InputDirectory @SkipWhenEmpty private File inputPath;

  public File getInputPath() {
    return inputPath;
  }

  public void setInputPath(File inputPath) {
    this.inputPath = inputPath;
  }

  @Input private List<String> types;

  public List<String> getTypes() {
    return types;
  }

  public void setTypes(List<String> types) {
    this.types = types;
  }

  @Input private String header;

  public String getHeader() {
    return header;
  }

  public void setHeader(String header) {
    this.header = header;
  }

  private boolean preview;

  @Option(option = "preview", description = "Don’t actually write/delete anything, just print")
  public void setPreview(boolean preview) {
    this.preview = preview;
  }

  @TaskAction
  public void generate() throws IOException {
    if (StringUtils.isEmpty(version)) {
      throw new GradleException("Version is missing. Please provide `changelog.version` property.");
    }

    List<ChangelogEntry> entries = getChangelogEntries();

    if (ObjectUtils.isEmpty(entries)) {
      getLogger().lifecycle("No unreleased changelog entries to process");
      return;
    }

    String newChangelog = generate(entries);

    if (preview) {
      getLogger().lifecycle("Generated changelog: ");
      getLogger().lifecycle("--------------------");
      getLogger().lifecycle(newChangelog);
      getLogger().lifecycle("--------------------");
      return;
    }

    write(newChangelog);
    clean(entries);
  }

  private List<ChangelogEntry> getChangelogEntries() throws IOException {
    getLogger().lifecycle("Processing unreleased changelog entries");
    ChangelogEntryParser parser = new ChangelogEntryParser();
    List<ChangelogEntry> entries = new ArrayList<>();
    for (File file : getFiles()) {
      getLogger().debug("Processing {}", file);
      entries.add(parser.parse(file));
    }
    return entries;
  }

  private String generate(List<ChangelogEntry> entries) {
    ReleaseProcessor processor = new ReleaseProcessor();
    Release release = processor.process(entries, version, header, types);

    ReleaseGenerator generator = new ReleaseGenerator();
    return generator.generate(release);
  }

  private void write(String newChangelog) throws IOException {
    getLogger().lifecycle("Generating new " + changelogPath.getName() + " file");

    if (!changelogPath.exists()) {
      Files.createFile(changelogPath.toPath());
    }

    StringBuilder contentBuilder = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new FileReader(changelogPath))) {

      String sCurrentLine;
      while ((sCurrentLine = br.readLine()) != null) {
        contentBuilder.append(sCurrentLine).append(System.lineSeparator());
      }
    }

    Files.deleteIfExists(changelogPath.toPath());

    try (FileOutputStream fos = new FileOutputStream(changelogPath)) {
      fos.write((newChangelog + System.lineSeparator() + contentBuilder.toString()).getBytes());
      fos.flush();
    }
  }

  private void clean(List<ChangelogEntry> entries) {
    getLogger().lifecycle("Clean up unreleased changelog entries");
    for (ChangelogEntry entry : entries) {
      Path path = entry.getPath();
      try {
        getLogger().trace("Deleting {}", path);
        Files.delete(path);
      } catch (IOException ex) {
        throw new GradleException("Could not delete file: " + path, ex);
      }
    }
  }

  private List<File> getFiles() {
    return Stream.of(Objects.requireNonNull(inputPath.listFiles()))
        .filter(
            file ->
                !file.isDirectory()
                    && (file.toString().endsWith(".yml") || file.toString().endsWith(".yaml")))
        .collect(Collectors.toList());
  }
}
