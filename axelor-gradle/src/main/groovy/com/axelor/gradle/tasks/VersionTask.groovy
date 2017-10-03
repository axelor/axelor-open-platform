/**
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
package com.axelor.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

import com.axelor.common.VersionUtils

class VersionTask extends DefaultTask {

	private static final def XML_PATTERN = /(domain-models|object-views|data-import)_\d+\.\d+\.xsd/
	private static final def JSON_PATTERN = /"version".*,/
	private static final def XSD_PATTERN = /version="\d+\.\d+"\>/

	private String version = VersionUtils.getVersion().version
	private String feature = VersionUtils.getVersion().feature

	@InputFiles
	@SkipWhenEmpty
	ConfigurableFileTree processFiles

	@TaskAction
	def update() {
		processFiles.each {
			this.processFile(it)
		}
	}

	void processFile(File file) {

		def name = file.name;
		def str = file.getText('UTF-8')
		def txt = str;
		if (name.endsWith('.xsd')) txt = process_xsd(txt)
		if (name.endsWith('.xml')) txt = process_xml(txt)
		if (name.endsWith('.tmpl')) txt = process_xml(txt)
		if (name.endsWith('.json')) txt = process_json(txt)

		if (str == txt) return

		println("Processing $file")
		file.write(txt, 'UTF-8')
	}

	private String process_xml(String text) {
		return text.replaceAll(XML_PATTERN, "\$1_${feature}.xsd")
	}

	private String process_xsd(String text) {
		return text.replaceAll(XSD_PATTERN, "version=\"${feature}\">")
	}

	private String process_json(String text) {
		return text.replaceAll(JSON_PATTERN, "\"version\": \"${version}\",")
	}
}
