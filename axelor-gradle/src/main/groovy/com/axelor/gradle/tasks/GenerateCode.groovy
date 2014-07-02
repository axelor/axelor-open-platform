package com.axelor.gradle.tasks

import com.axelor.tools.x2j.Generator
import com.axelor.tools.x2j.Extender

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.api.artifacts.ProjectDependency

class GenerateCode extends DefaultTask {

	String base
	String target

	List<String> IGNORE = ["axelor-common", "axelor-test"]

	@TaskAction
	def generate() {
		generateInfo()
		generateCode()
	}

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
			if (!found.contains(dep) && !IGNORE.contains(dep.name)) {
				found.add(dep)
				findAllModules(dep, found)
			}
		}
		return found
	}

	def generateCode() {
		
		def definition = project.extensions.findByName("module")
		def generator = new Generator(base, target)

		if (IGNORE.contains(definition.name)) {
			return
		}

		generator.start()

		def objects = new File(project.projectDir, "src/main/resources/objects")
		if (objects.exists()) {
			def all = findAllModules(project, null).collect { it.projectDir }
			def extender = new Extender(base, target, all)
			extender.start()
		}
	}

	def generateInfo() {
		
		def definition = project.extensions.findByName("module")
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

			project.configurations.compile.allDependencies.withType(ProjectDependency).each {
				def dep = it.dependencyProject
				if (dep.isModule && !IGNORE.contains(dep.name)) {
					deps += "\t" + dep.name
				}
			}

			out << """\
name = ${definition.name}
version = ${project.version}

title = ${definition.title?:""}
description = ${definition.description?:""}

removable = ${definition.removable == true}

depends = \\
${deps.join("\\\n")}
"""
		}
	}
}
