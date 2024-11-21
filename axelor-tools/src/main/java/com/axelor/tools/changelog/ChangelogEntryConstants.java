/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
