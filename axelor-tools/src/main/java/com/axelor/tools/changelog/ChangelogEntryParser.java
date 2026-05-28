/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.changelog;

import com.axelor.common.ObjectUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class ChangelogEntryParser {

  public ChangelogEntry parse(File file) throws IOException {
    Map<String, Object> values = loadYaml(file);
    if (ObjectUtils.isEmpty(values)) {
      throw new IllegalStateException(file + " content is empty");
    }
    return createEntry(values, file);
  }

  private Map<String, Object> loadYaml(File file) throws IOException {
    Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
    try (InputStream ios = new FileInputStream(file)) {
      return yaml.load(ios);
    }
  }

  private ChangelogEntry createEntry(Map<String, Object> entries, File file) {
    ChangelogEntry changelogEntry = new ChangelogEntry();
    for (Map.Entry<String, Object> item : entries.entrySet()) {
      String value = item.getValue().toString();
      if (value == null) continue;
      if ("title".equalsIgnoreCase(item.getKey())) {
        changelogEntry.setTitle(value.trim());
      } else if ("description".equalsIgnoreCase(item.getKey())) {
        changelogEntry.setDescription(value.trim());
      } else if ("type".equalsIgnoreCase(item.getKey())) {
        changelogEntry.setType(value);
      }
    }
    changelogEntry.setPath(file.toPath());
    return changelogEntry;
  }
}
