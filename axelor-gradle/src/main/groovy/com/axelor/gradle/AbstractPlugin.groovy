/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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
package com.axelor.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

import com.axelor.common.VersionUtils
import com.axelor.gradle.tasks.I18nTask

abstract class AbstractPlugin implements Plugin<Project> {

	String sdkVersion
	
	protected String getSdkVersion() {
		if (sdkVersion) {
			return sdkVersion
		}
		return sdkVersion = VersionUtils.getVersion().version;
	}
	
	protected void checkVersion(Project project, AbstractDefinition definition) {
		if (!definition.adkVersion || VersionUtils.version.matches(definition.adkVersion)) {
			return;
		}
		throw new GradleException(
			String.format("ADK version mismatch, '%s' requires: %s",
				definition.name, definition.adkVersion));
	}

	protected void applyCommon(Project project, AbstractDefinition definition) {

		project.task('i18n-extract', type: I18nTask) {
			description "Extract i18n messages from source files."
			group "Axelor"
			update = false
			withContext = project.hasProperty('with.context')
		}

		project.task('i18n-update', type: I18nTask) {
			description "Update i18 messages from message catalog."
			group "Axelor"
			update = true
			withContext = project.hasProperty('with.context')
		}

		project.afterEvaluate {
			// add module dependency
			definition.modules.each { module ->
				project.dependencies {
					compile project.project(":${module}")
				}
			}
		}
	}
}
