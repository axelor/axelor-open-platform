package com.axelor.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

import com.axelor.gradle.tasks.GenerateCode

class ModulePlugin extends BasePlugin {

	void apply(Project project) {

		super.apply(project)

		project.configure(project) {

			// add dependency to some base modules
			afterEvaluate {
				dependencies {
					compile project.project(":core:axelor-core")
				}
			}
        }
    }
}

