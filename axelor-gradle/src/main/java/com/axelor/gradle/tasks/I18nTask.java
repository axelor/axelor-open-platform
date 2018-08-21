/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
package com.axelor.gradle.tasks;

import com.axelor.gradle.AxelorPlugin;
import com.axelor.tools.i18n.I18nExtractor;
import java.nio.file.Paths;
import org.gradle.api.DefaultTask;
import org.gradle.api.internal.tasks.options.Option;
import org.gradle.api.tasks.TaskAction;

public class I18nTask extends DefaultTask {

  public static final String TASK_NAME = "i18n";
  public static final String TASK_DESCRIPTION = "Update i18n messages from source files.";
  public static final String TASK_GROUP = AxelorPlugin.AXELOR_BUILD_GROUP;

  private boolean withContext;

  @Option(option = "with-context", description = "Specify whether to include context details.")
  public void setWithContext(boolean withContext) {
    this.withContext = withContext;
  }

  @TaskAction
  public void extract() {
    final I18nExtractor extractor = new I18nExtractor();
    extractor.extract(Paths.get(getProject().getProjectDir().getPath()), true, withContext);
  }
}
