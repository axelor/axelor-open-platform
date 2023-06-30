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
package com.axelor.tools.changelog;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReleaseProcessor {

  public Release process(
      Collection<ChangelogEntry> changelogEntries,
      String version,
      String header,
      List<String> types) {

    Objects.requireNonNull(version);
    Objects.requireNonNull(header);
    Objects.requireNonNull(changelogEntries);

    validate(changelogEntries);
    adjustEntriesTypes(changelogEntries, types);

    Release release = new Release();
    release.setVersion(version);
    release.setHeader(header);

    LinkedHashMap<String, List<ChangelogEntry>> entriesGroupedByType =
        changelogEntries.stream()
            .sorted(Comparator.comparingInt(e -> types.indexOf(e.getType())))
            .collect(
                Collectors.groupingBy(
                    ChangelogEntry::getType, LinkedHashMap::new, Collectors.toList()));
    release.setEntries(entriesGroupedByType);

    return release;
  }

  private void adjustEntriesTypes(Collection<ChangelogEntry> changelogEntries, List<String> types) {
    for (ChangelogEntry changelogEntry : changelogEntries) {
      changelogEntry.setType(getTargetType(changelogEntry.getType(), types));
    }
  }

  private String getTargetType(String type, List<String> types) {
    for (String targetType : types) {
      if (type.equalsIgnoreCase(targetType)) {
        return targetType;
      }
    }
    throw new IllegalArgumentException(
        String.format("Type %s cannot be found in %s", type, String.join(",", types)));
  }

  private void validate(Collection<ChangelogEntry> changelogEntries) {
    Objects.requireNonNull(changelogEntries);

    Optional<ChangelogEntry> entryWithNullType =
        changelogEntries.stream().filter(entry -> entry.getType() == null).findFirst();
    if (entryWithNullType.isPresent()) {
      throw new IllegalArgumentException(
          "Type cannot be null in changelog entry: " + entryWithNullType.get());
    }

    Optional<ChangelogEntry> entryWithNullTitle =
        changelogEntries.stream().filter(entry -> entry.getTitle() == null).findFirst();
    if (entryWithNullTitle.isPresent()) {
      throw new IllegalArgumentException(
          "Title cannot be null in changelog entry: " + entryWithNullTitle.get());
    }
  }
}
