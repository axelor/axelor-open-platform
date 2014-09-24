/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.TaskAction

import com.axelor.tools.x2j.Generator

class GenerateCode extends DefaultTask {

	def findAllModules(Project project, Set<Project> found) {
		def name = project.name
		if (found == null) {
			found = []
		}
		project.configurations.compile.allDependencies.withType(ProjectDependency).each {
			def dep = it.dependencyProject
			if (!found.contains(dep)) {
				found.add(dep)
				findAllModules(dep, found)
			}
		}
		return found
	}

	def findCoreFiles(Project project) {
		def all = project.configurations.compile.allDependencies.withType(ExternalModuleDependency)
			.findAll { ['axelor-core', 'axelor-web'].contains(it.name) }
			.collect { it.name }

		if (all == null || all.empty) {
			return null
		}
		return project.configurations.compile
			.findAll { f -> all.find { n -> f.name.startsWith n } }
			.collect { project.rootProject.zipTree(it).matching { include "**/domains/**" }.files }
			.flatten()
	}

	@TaskAction
	def generate() {
		generateCode("module")
		generateCode("application")
	}
	
	def expandAll() {
		def outputPath = new File(project.rootProject.buildDir, "src-gen")
		Generator.combineAll(outputPath)
	}

	def buildGenerator(Project project) {
		def domainPath = new File(project.projectDir, "src/main/resources/domains")
		def targetPath = new File(project.buildDir, "src-gen")
		def generator = new Generator(domainPath, targetPath)
		return generator
	}

	def generateCode(String extension) {

		def definition = project.extensions.findByName(extension)
		if (definition == null) {
			return
		}

		generateInfo(extension)

		def generator = buildGenerator(project)

		def coreFiles = findCoreFiles(project)
		if (coreFiles && !coreFiles.empty) {
			generator.addLookupSource(Generator.forFiles(coreFiles))
		}

		findAllModules(project, null).each {
			def lookup = buildGenerator(it)
			generator.addLookupSource(lookup)
		}
		
		generator.start()

		// copy module.properties
		project.copy {
			from "${project.buildDir}/src-gen/module.properties"
			into "${project.buildDir}/classes/main"
		}
	}

	def generateInfo(String extension) {
		
		def definition = project.extensions.findByName(extension)
		if (definition == null) {
			return
		}

		def outputPath = new File(project.buildDir, "src-gen/module.properties")
		try {
			outputPath.parentFile.mkdirs()
		} catch (Exception e) {
			logger.info("Error generating module.properties", e);
		}

		logger.info("Generating: {}", outputPath.parent)

		outputPath.withWriter('UTF-8') { out ->
			
			def desc = (definition.description?:"").replaceAll("\n", "\\\n")
			def deps = []
			def removable = false

			try {
				removable = definition.removable
			} catch (Exception e) {
			}

			findAllModules(project, null).each {
				deps += "\t" + it.name
			}

			out << """\
name = ${definition.name}
version = ${project.version}

title = ${definition.title?:""}
description = ${desc}

"""
			if (removable) {
				out << """\
removable = ${removable}

"""
			}

			out << """\
depends = \\
${deps.join(" \\\n")}
"""
		}
	}
}
