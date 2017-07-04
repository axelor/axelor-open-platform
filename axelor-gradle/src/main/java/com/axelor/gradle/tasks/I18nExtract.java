/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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

import java.nio.file.Paths;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.axelor.gradle.AxelorPlugin;
import com.axelor.tools.i18n.I18nExtractor;

public class I18nExtract extends DefaultTask {

	public static final String TASK_NAME = "i18nExtract";
	public static final String TASK_DESCRIPTION = "Extract i18n messages from source files.";
	public static final String TASK_GROUP = AxelorPlugin.AXELOR_BUILD_GROUP;

	@TaskAction
	public void extract() {
		final I18nExtractor extractor = new I18nExtractor();
		final boolean withContext = getProject().hasProperty("with.context");
		extractor.extract(Paths.get(getProject().getProjectDir().getPath()), true, withContext);
	}
}
