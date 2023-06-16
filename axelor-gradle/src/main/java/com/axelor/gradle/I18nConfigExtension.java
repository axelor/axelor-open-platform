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
package com.axelor.gradle;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class I18nConfigExtension {

  public static final String EXTENSION_NAME = "i18nConfig";

  private List<Path> extraSources;

  public List<Path> getExtraSources() {
    return extraSources;
  }

  public void setExtraSources(List<CharSequence> extraSrc) {
    this.extraSources =
        extraSrc.stream()
            .map(src -> Paths.get(src.toString()))
            .collect(Collectors.toUnmodifiableList());
  }
}
