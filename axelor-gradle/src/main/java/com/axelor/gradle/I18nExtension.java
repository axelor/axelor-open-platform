/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.gradle;

import java.nio.file.Path;
import java.util.List;

public class I18nExtension {

  public static final String EXTENSION_NAME = "i18n";

  private List<Path> extraSources;

  public List<Path> getExtraSources() {
    return extraSources;
  }

  public void setExtraSources(List<CharSequence> extraSrc) {
    this.extraSources = extraSrc.stream().map(src -> Path.of(src.toString())).toList();
  }
}
