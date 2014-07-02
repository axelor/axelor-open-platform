package com.axelor.gradle.tasks

import java.nio.file.Paths

import com.axelor.tools.i18n.I18nExtractor

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class I18nTask extends DefaultTask {

	@TaskAction
	def extract() {
		def extractor = new I18nExtractor()
		extractor.extract(Paths.get(project.projectDir.path))
	}
}
