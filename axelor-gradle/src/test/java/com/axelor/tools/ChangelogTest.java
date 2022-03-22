/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.axelor.common.ResourceUtils;
import com.axelor.tools.changelog.ChangelogEntry;
import com.axelor.tools.changelog.ChangelogEntryParser;
import com.axelor.tools.changelog.Release;
import com.axelor.tools.changelog.ReleaseGenerator;
import com.axelor.tools.changelog.ReleaseProcessor;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class ChangelogTest {

  @Test
  public void generateChangelogTest() throws URISyntaxException, IOException {
    ReleaseProcessor processor = new ReleaseProcessor();
    Release release = processor.process(getEntries(), "1.0.0", LocalDate.of(2020, 1, 10));

    File outputFile =
        Paths.get(ResourceUtils.getResource("changelogs/EXPECTED_CHANGELOG.md").toURI()).toFile();
    String output =
        com.google.common.io.Files.asCharSource(outputFile, StandardCharsets.UTF_8).read();

    assertEquals(output, new ReleaseGenerator().generate(release));
  }

  List<ChangelogEntry> getEntries() throws URISyntaxException, IOException {
    URL changelogsUrl = ResourceUtils.getResource("changelogs/entries/");
    ChangelogEntryParser parser = new ChangelogEntryParser();

    try (Stream<Path> stream = Files.list(Paths.get(changelogsUrl.toURI()))) {
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
}
