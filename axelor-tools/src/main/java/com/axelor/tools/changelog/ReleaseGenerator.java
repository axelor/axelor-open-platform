/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.changelog;

import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.google.common.base.Strings;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReleaseGenerator {

  private static final String NEW_LINE = System.lineSeparator();

  public String generate(Release release) {
    StringBuilder releaseContent = new StringBuilder();

    appendHeader(releaseContent, release);
    appendEntries(releaseContent, release);
    if (ObjectUtils.isEmpty(release.getEntries())) {
      if (ObjectUtils.notEmpty(release.getDefaultContent())) {
        if (!endWithEmptyLine(releaseContent.toString())) {
          releaseContent.append(NEW_LINE);
        }
        releaseContent.append(release.getDefaultContent()).append(NEW_LINE);
      }
    }

    return releaseContent.toString();
  }

  private void appendEntries(StringBuilder content, Release release) {
    if (release.getEntries() == null) {
      return;
    }

    for (String type : release.getEntries().keySet()) {
      appendEntriesPerType(content, type, release.getEntries().get(type));
    }
  }

  private void appendEntriesPerType(
      StringBuilder content, String type, List<ChangelogEntry> entries) {
    if (entries == null || entries.isEmpty()) {
      return;
    }
    if (!endWithEmptyLine(content.toString())) {
      content.append(NEW_LINE);
    }
    content.append("#### ").append(type).append(NEW_LINE).append(NEW_LINE);
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
        .append(MessageFormat.format("## {0}", release.getHeader()))
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
