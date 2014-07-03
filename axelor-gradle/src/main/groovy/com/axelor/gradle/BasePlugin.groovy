package com.axelor.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

import com.axelor.gradle.tasks.GenerateCode

class BasePlugin extends AbstractPlugin {

	void apply(Project project) {
        
		project.configure(project) {

			def definition = extensions.create("module", ModuleDefinition)

			applyCommon(project, definition)

			ext.isModule = true

			// add code generation tasl
			task("generateCode", type: GenerateCode) {
				base = project.projectDir
				target = rootProject.buildDir
			}

			compileJava.dependsOn "generateCode"

			// don't include class & webapp files in jar
			jar {
				exclude(['**/*.class', 'webapp'])
				includeEmptyDirs = false
			}

			task("copyClasses", type: org.gradle.api.tasks.Copy) {
				from "${buildDir}/classes"
				into "${rootProject.buildDir}/classes"
			}

			jar.dependsOn "copyClasses"	
        }
    }
}

