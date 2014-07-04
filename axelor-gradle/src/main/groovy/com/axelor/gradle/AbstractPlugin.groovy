package com.axelor.gradle

import com.axelor.gradle.tasks.I18nTask

import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class AbstractPlugin implements Plugin<Project> {
    
	protected void applyCommon(Project project, AbstractDefinition definition) {

		project.configure(project) {

			apply plugin: 'java'

			apply from: rootDir.path + '/core/libs.gradle'
			apply from: rootDir.path + '/core/repo.gradle'

			sourceCompatibility = 1.7
			targetCompatibility = 1.7

			dependencies {
				compile libs.slf4j
				testCompile	libs.junit
			}

			task('i18n-extract', type: I18nTask) {

			}

			afterEvaluate {
	
				// add module dependency
				definition.modules.each { module ->
					dependencies {
						compile project.project(":${module}")
					}
                }

				// force groovy compiler
				if (plugins.hasPlugin("groovy")) {
					sourceSets {
						main {
							java.srcDirs = []
							groovy.srcDirs = ['src/main', rootDir.path + '/build/src-gen']
						}
						test {
							java.srcDirs = []
							groovy.srcDirs = ['src/main', rootDir.path + '/build/src-gen']
						}
					}
				}

				// add generated source/classes to compile classpath
				sourceSets.main.compileClasspath += files([rootDir.path + '/build/src-gen'])
				sourceSets.main.compileClasspath += files([rootDir.path + '/build/classes/main'])
            }
		}
	}
}
