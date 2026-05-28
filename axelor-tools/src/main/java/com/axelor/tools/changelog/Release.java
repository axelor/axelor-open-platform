/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.changelog;

import java.util.List;
import java.util.Map;

public class Release {

  private String version;
  private String header;
  private String defaultContent;
  private Map<String, List<ChangelogEntry>> entries;

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getHeader() {
    return header;
  }

  public void setHeader(String header) {
    this.header = header;
  }

  public Map<String, List<ChangelogEntry>> getEntries() {
    return entries;
  }

  public void setEntries(Map<String, List<ChangelogEntry>> entries) {
    this.entries = entries;
  }

  public void setDefaultContent(String defaultContent) {
    this.defaultContent = defaultContent;
  }

  public String getDefaultContent() {
    return defaultContent;
  }
}
