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
package com.axelor.gradle.tasks;

import com.axelor.common.ObjectUtils;
import com.axelor.common.VersionUtils;
import com.axelor.tools.changelog.ChangelogEntry;
import com.axelor.tools.changelog.ChangelogEntryParser;
import com.axelor.tools.changelog.Release;
import com.axelor.tools.changelog.ReleaseGenerator;
import com.axelor.tools.changelog.ReleaseProcessor;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.tasks.options.Option;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class GenerateChangeLog extends DefaultTask {

  private final Logger LOG = LoggerFactory.getLogger(this.getClass());

  private static final String CHANGELOG_FILENAME = "CHANGELOG.md";

  private String version = VersionUtils.getVersion().version.replace("-SNAPSHOT", "");

  private boolean preview;

  @Option(option = "preview", description = "Preview mode.")
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
    if(ObjectUtils.isEmpty(entries)) {
      getLogger().info("No change log entries to process. Skipping.");
      return;
    }

    String newChangelog = generate(entries);

    if(preview) {
      System.out.println("Generated change log : ");
      System.out.println("--------------------");
      System.out.println(newChangelog);
      System.out.println("--------------------");
      return;
    }

    write(newChangelog);
    clean();
  }

  private List<ChangelogEntry> getChangelogEntries() throws IOException {
    ChangelogEntryParser parser = new ChangelogEntryParser();
    List<ChangelogEntry> entries = new ArrayList<>();
    for (File file : getFiles()) {
      entries.add(parser.parse(file));
    }
    return entries;
  }

  private String generate(List<ChangelogEntry> entries) {
    ReleaseProcessor processor = new ReleaseProcessor();
    Release release = processor.process(entries, version);

    ReleaseGenerator generator = new ReleaseGenerator();
    return generator.generate(release);
  }

  private void write(String newChangelog) throws IOException {
    File mFile = new File(CHANGELOG_FILENAME);

    StringBuilder contentBuilder = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new FileReader(mFile))) {

      String sCurrentLine;
      while ((sCurrentLine = br.readLine()) != null) {
        contentBuilder.append(sCurrentLine).append(System.lineSeparator());
      }
    }

    mFile.delete();

    try (FileOutputStream fos = new FileOutputStream(mFile)) {
      fos.write((newChangelog + contentBuilder.toString()).getBytes());
      fos.flush();
    }
  }

  private void clean() {
    for (File file : getFiles()) {
      try {
        Files.delete(file.toPath());
      } catch (IOException ex) {
        throw new GradleException("Could not delete file: " + file, ex);
      }
    }
  }
}
