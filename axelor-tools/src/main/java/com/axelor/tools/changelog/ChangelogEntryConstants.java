/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.changelog;

import java.util.List;

public final class ChangelogEntryConstants {

  private ChangelogEntryConstants() {}

  public static final String CHANGELOG_FILE = "CHANGELOG.md";
  public static final String INPUT_PATH = "changelogs/unreleased";

  public static final List<String> TYPES =
      List.of("Feature", "Change", "Deprecate", "Remove", "Fix", "Security");

  public static final boolean ALLOW_NO_ENTRY = false;
  public static final String DEFAULT_CONTENT = "";
}
