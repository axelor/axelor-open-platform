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
import com.axelor.common.VersionUtils;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

public class GenerateChangelog extends DefaultTask {

  private static final String CHANGELOG_PATH = "CHANGELOG.md";

  private String version = VersionUtils.getVersion().version.replace("-SNAPSHOT", "");

  private boolean preview;

  @Option(option = "preview", description = "Don’t actually write/delete anything, just print")
  public void setPreview(boolean preview) {
    this.preview = preview;
  }

  @InputFiles @SkipWhenEmpty private FileTree files;

  public FileTree getFiles() {
    return files;
  }

  public void setFiles(FileTree files) {
    this.files = files;
  }

  @TaskAction
  public void generate() throws IOException {
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
    clean();
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
    Release release = processor.process(entries, version, LocalDate.now());

    ReleaseGenerator generator = new ReleaseGenerator();
    return generator.generate(release);
  }

  private void write(String newChangelog) throws IOException {
    getLogger().lifecycle("Generating new CHANGELOG.md file");

    File changelogFile = new File(CHANGELOG_PATH);

    StringBuilder contentBuilder = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new FileReader(changelogFile))) {

      String sCurrentLine;
      while ((sCurrentLine = br.readLine()) != null) {
        contentBuilder.append(sCurrentLine).append(System.lineSeparator());
      }
    }

    changelogFile.delete();

    try (FileOutputStream fos = new FileOutputStream(changelogFile)) {
      fos.write((newChangelog + System.lineSeparator() + contentBuilder.toString()).getBytes());
      fos.flush();
    }
  }

  private void clean() {
    getLogger().lifecycle("Clean up unreleased changelog entries");
    for (File file : getFiles()) {
      try {
        getLogger().lifecycle("Deleting {}", file);
        Files.delete(file.toPath());
      } catch (IOException ex) {
        throw new GradleException("Could not delete file: " + file, ex);
      }
    }
  }
}
