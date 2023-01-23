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

import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.google.common.base.Strings;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReleaseGenerator {

  private static final String NEW_LINE = System.lineSeparator();

  public String generate(Release release) {
    StringBuilder releaseContent = new StringBuilder();

    appendHeader(releaseContent, release);
    appendEntries(releaseContent, release);

    return releaseContent.toString();
  }

  private void appendEntries(StringBuilder content, Release release) {
    if (release.getEntries() == null) {
      return;
    }
    SortedMap<String, EntryType> sortedTypes = new TreeMap<>();
    for (EntryType type : EntryType.values()) {
      sortedTypes.put(type.getValue(), type);
    }

    for (EntryType type : sortedTypes.values()) {
      if (release.getEntries().containsKey(type)) {
        appendEntriesPerType(content, type, release.getEntries().get(type));
      }
    }
  }

  private void appendEntriesPerType(
      StringBuilder content, EntryType type, List<ChangelogEntry> entries) {
    if (entries == null || entries.isEmpty()) {
      return;
    }
    if (!endWithEmptyLine(content.toString())) {
      content.append(NEW_LINE);
    }
    content.append("#### ").append(type.getValue()).append(NEW_LINE).append(NEW_LINE);
    for (ChangelogEntry entry : entries) {
      content.append(MessageFormat.format("* {0}", entry.getTitle()));
      if (!StringUtils.isEmpty(entry.getDescription())) {
        content
            .append(NEW_LINE)
            .append(NEW_LINE)
            .append(new EntryDescriptionGenerator(entry.getDescription()).generate());
      }
      content.append(NEW_LINE);
    }
  }

  private boolean endWithEmptyLine(String content) {
    return NEW_LINE.equals(Character.toString(content.charAt(content.length() - 1)))
        && NEW_LINE.equals(Character.toString(content.charAt(content.length() - 2)));
  }

  private void appendHeader(StringBuilder content, Release release) {
    content
        .append(MessageFormat.format("## {0} ({1})", release.getVersion(), release.getDate()))
        .append(NEW_LINE)
        .append(NEW_LINE);
  }

  static class EntryDescriptionGenerator {

    private final String content;

    public EntryDescriptionGenerator(String content) {
      this.content = content;
    }

    public String generate() {
      List<String> lines = new ArrayList<>();
      lines.add("<details>");
      lines.add("");
      for (String line : content.trim().split(NEW_LINE)) {
        lines.add(StringUtils.isBlank(line) ? "" : line);
      }
      lines.add("");
      lines.add("</details>");
      return indent(String.join(NEW_LINE, lines), 2);
    }

    private String indent(String text, int n) {
      if (ObjectUtils.isEmpty(text)) {
        return "";
      }
      Stream<String> stream = Arrays.stream(text.split(NEW_LINE));
      if (n > 0) {
        final String spaces = Strings.repeat(" ", n);
        stream = stream.map(s -> spaces + s);
      }
      return stream.collect(Collectors.joining(NEW_LINE, "", NEW_LINE));
    }
  }
}
