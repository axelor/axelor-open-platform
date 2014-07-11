package com.axelor.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.TaskAction

import com.axelor.tools.x2j.Generator

class GenerateCode extends DefaultTask {

	String base
	String target

	List<String> IGNORE = ["axelor-common", "axelor-test"]

	def findAllModules(Project project, Set<Project> found) {
		def name = project.name
		if (found == null) {
			found = []
		}
		if (IGNORE.contains(name)) {
			return found
		}
		project.configurations.compile.allDependencies.withType(ProjectDependency).each {
			def dep = it.dependencyProject
			if (!found.contains(dep) && !IGNORE.contains(dep.name) && dep.isModule) {
				found.add(dep)
				findAllModules(dep, found)
			}
		}
		return found
	}

	@TaskAction
	def generate() {
		generateCode("module")
		generateCode("application")
	}

	def generateCode(String extension) {

		def definition = project.extensions.findByName(extension)
		if (definition == null || IGNORE.contains(definition.name)) {
			return
		}

		generateInfo(extension)

		def generator = new Generator(base, target)
		generator.start()
	}

	def generateInfo(String extension) {
		
		def definition = project.extensions.findByName(extension)
		if (definition == null) {
			return
		}

		def outputPath = new File(project.buildDir, "resources/main/module.properties")
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
description = ${definition.description?:""}

"""
			if (removable) {
				out << """\
removable = ${definition.removable == true}

"""
			}

			out << """\
depends = \\
${deps.join("\\\n")}
"""
		}
	}
}
