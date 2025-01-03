/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
package com.axelor.gradle.tasks;

import com.axelor.common.StringUtils;
import com.axelor.gradle.AxelorPlugin;
import com.axelor.gradle.I18nExtension;
import com.axelor.tools.i18n.I18nExtractor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

public class I18nTask extends DefaultTask {

  public static final String TASK_NAME = "i18n";
  public static final String TASK_DESCRIPTION = "Update i18n messages from source files.";
  public static final String TASK_GROUP = AxelorPlugin.AXELOR_BUILD_GROUP;

  private boolean withContext;

  private List<String> languages = Collections.emptyList();

  @Option(option = "with-context", description = "Specify whether to include context details.")
  public void setWithContext(boolean withContext) {
    this.withContext = withContext;
  }

  @Option(
      option = "languages",
      description = "comma-separated list of additional languages files to create")
  public void setLanguages(String languages) {
    this.languages =
        Arrays.stream(languages.split("\\s*,\\s*"))
            .map(StringUtils::normalizeLanguageTag)
            .collect(Collectors.toUnmodifiableList());
  }

  @TaskAction
  public void extract() {
    final I18nExtractor extractor = new I18nExtractor();
    final Path base = Paths.get(getProject().getProjectDir().getPath());
    final Path src = base.resolve(Paths.get("src", "main"));
    final Path dest = src.resolve("resources");
    final boolean update = true;

    final List<Path> srcList = new ArrayList<>();
    srcList.add(src);

    final I18nExtension extension = getProject().getExtensions().findByType(I18nExtension.class);
    if (extension != null) {
      final List<Path> extraSources = extension.getExtraSources();
      if (extraSources != null) {
        for (final Path extraSource : extraSources) {
          srcList.add(base.resolve(extraSource));
        }
      }
    }

    extractor.extract(srcList, dest, update, withContext, languages);
  }
}
