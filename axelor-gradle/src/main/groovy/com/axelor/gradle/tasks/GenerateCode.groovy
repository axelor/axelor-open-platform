/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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

	private static final Set<String> IGNORE = ["axelor-common", "axelor-test"]
	private static final List<String> INCLUDE = ["axelor-core", "axelor-wkf"]

	def _findDeps(Project project) {
		return project.configurations.compile.allDependencies.withType(ProjectDependency).collect { it.dependencyProject }
	}

	def _findAllModules(Project project, Map visited) {
		def found = []
		def all = visited.get(project.name)
		if (all == null) {
			all = _findDeps(project)
			visited[project.name] = all
		}

		all.each { dep ->
			if (!found.contains(dep) && !IGNORE.contains(dep.name)) {
				found.add(dep)
				found.addAll(_findAllModules(dep, visited))
			}
		}

		return found.unique()
	}

	/**
	 * Find all the module dependencies in topological order
	 *
	 */
	def findAllModules(Project project) {

		def graph = [:]
		def found = _findAllModules(project, graph)

		found.each {
			if (!graph.containsKey(it.name)) graph[it.name] = _findDeps(it)
		}

		def all = found.sort { a, b ->
			def ad = graph.get(a.name)
			def bd = graph.get(b.name)
			if (ad.contains(b)) return 1;
			if (bd.contains(a)) return -1;
			return 0;
		}

		return all
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

		findAllModules(project).each {
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
			
			def description = (definition.description?:"").trim().split("\n").collect { it.trim() };
			def depends = findAllModules(project).collect { it.name }
			def installs = null
			def removable = false
			
			if (!INCLUDE.contains(project.name)) {
				depends = INCLUDE + depends
				depends = depends.unique()
			}

			try {
				removable = definition.removable
			} catch (Exception e) {
			}

			try {
				installs = definition.installs
			} catch (Exception e) {
			}

			out << """\
name = ${definition.name}
version = ${project.version}

title = ${definition.title?:""}
description = ${description.join("\\n")}
"""
			if (removable) {
				out << """\

removable = ${removable}
"""
			}

			out << """\

depends = ${depends.join(", ")}
"""
			if (installs) {
				out << """\

installs = ${installs.join(", ")}
"""
			}
		}
	}
}
