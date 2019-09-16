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
package com.axelor.tools.changelog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public class ChangelogEntryParser {

  public ChangelogEntry parse(File file) throws IOException {
    return createEntry(loadYaml(file));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> loadYaml(File file) throws IOException {
    Yaml yaml = new Yaml();
    try (InputStream ios = new FileInputStream(file)) {
      return (Map<String, Object>) yaml.load(ios);
    }
  }

  private ChangelogEntry createEntry(Map<String, Object> entries) {
    ChangelogEntry changelogEntry = new ChangelogEntry();
    for (Map.Entry<String, Object> item : entries.entrySet()) {
      String value = item.getValue().toString();
      if ("title".equalsIgnoreCase(item.getKey())) {
        changelogEntry.setTitle(value);
      } else if ("type".equalsIgnoreCase(item.getKey())) {
        changelogEntry.setType(EntryType.valueOf(value.toUpperCase()));
      }
    }
    return changelogEntry;
  }
}
