package com.axelor.gradle

import com.axelor.gradle.tasks.I18nTask

import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class AbstractPlugin implements Plugin<Project> {
    
	protected void applyCommon(Project project, AbstractDefinition definition) {

		project.configure(project) {

			apply plugin: 'java'
			apply plugin: 'groovy'
			apply plugin: 'eclipse'

			apply from: rootDir.path + '/core/libs.gradle'
			apply from: rootDir.path + '/core/repo.gradle'

			sourceCompatibility = 1.7
			targetCompatibility = 1.7

			dependencies {
				compile libs.slf4j
				compile libs.groovy
				testCompile	libs.junit
			}

			task('i18n-extract', type: I18nTask) {

			}

			tasks.eclipse.dependsOn "cleanEclipse"

			eclipse {
				classpath {
					defaultOutputDir = file("${buildDir}/eclipse")
				}
			}

			afterEvaluate {
	
				def useSrcGen = true
				try {
					useSrcGen = project.useSrcGen
				} catch (Exception e) {}

				// add module dependency
				definition.modules.each { module ->
					dependencies {
						compile project.project(":${module}")
					}
                }
				
				// add src-gen as source directory
				if (useSrcGen) {
					sourceSets {
						main {
							java {
								srcDir "${rootDir}/build/src-gen"
							}
						}
					}
				}

				// force groovy compiler
				if (plugins.hasPlugin("groovy")) {
					sourceSets {
						main {
							java {
								srcDirs = []
							}
							groovy {
								srcDirs = ["src/main/java", "src/main/groovy"]
								if (useSrcGen) {
									srcDir "${rootDir}/build/src-gen"
								}
							}
						}
						test {
							java {
								srcDirs = []
							}
							groovy {
								srcDirs = ["src/test/java", "src/test/groovy"]
							}
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
