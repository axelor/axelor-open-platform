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

			task("generateCode", type: GenerateCode)

			compileJava.dependsOn "generateCode"
        }
    }
}

