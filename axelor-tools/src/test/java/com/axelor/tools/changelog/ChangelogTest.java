/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.changelog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.axelor.common.ResourceUtils;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class ChangelogTest {

  @Test
  public void generateChangelogTest() throws URISyntaxException, IOException {
    String version = "1.0.0";
    String header = getHeader(version);

    ReleaseProcessor processor = new ReleaseProcessor();
    Release release =
        processor.process(getEntries(), version, header, ChangelogEntryConstants.TYPES, null);

    File outputFile =
        Path.of(ResourceUtils.getResource("changelogs/EXPECTED_CHANGELOG.md").toURI()).toFile();
    String output =
        com.google.common.io.Files.asCharSource(outputFile, StandardCharsets.UTF_8).read();

    assertEquals(output, new ReleaseGenerator().generate(release));
  }

  List<ChangelogEntry> getEntries() throws URISyntaxException, IOException {
    URL changelogsUrl = ResourceUtils.getResource("changelogs/entries/");
    ChangelogEntryParser parser = new ChangelogEntryParser();

    try (Stream<Path> stream = Files.list(Path.of(changelogsUrl.toURI())).sorted()) {
      return stream
          .map(Path::toFile)
          .map(
              it -> {
                try {
                  return parser.parse(it);
                } catch (IOException e) {
                  return null;
                }
              })
          .collect(Collectors.toList());
    }
  }

  String getHeader(String version) {
    return "%s (%s)"
        .formatted(version, LocalDate.of(2020, 1, 10).format(DateTimeFormatter.ISO_LOCAL_DATE));
  }

  @Test
  public void generateEmptyReleaseTest() {
    String version = "1.0.0";
    String defaultContent = "No Changes";
    String header = getHeader(version);

    ReleaseProcessor processor = new ReleaseProcessor();
    Release release =
        processor.process(Collections.emptyList(), version, header, null, defaultContent);

    assertEquals(
        "## 1.0.0 (2020-01-10)\n\n" + defaultContent + "\n",
        new ReleaseGenerator().generate(release));
  }
}
