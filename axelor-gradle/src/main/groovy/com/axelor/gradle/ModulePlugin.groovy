package com.axelor.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

import com.axelor.gradle.tasks.GenerateCode

class ModulePlugin extends BasePlugin {

	void apply(Project project) {

		super.apply(project)

		project.configure(project) {
			// add some common dependencies
			afterEvaluate {
				dependencies {
					compile project.project(":core:axelor-core")
					testCompile project.project(":core:axelor-test")
				}
			}
        }
    }
}

