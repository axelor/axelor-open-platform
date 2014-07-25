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
package com.axelor.gradle

import com.axelor.gradle.tasks.I18nTask

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.plugins.ide.eclipse.model.SourceFolder

abstract class AbstractPlugin implements Plugin<Project> {
    
	protected void applyCommon(Project project, AbstractDefinition definition) {

		project.configure(project) {

			apply plugin: 'java'
			apply plugin: 'groovy'
			apply plugin: 'eclipse'
			apply plugin: 'eclipse-wtp'

			apply from: rootDir.path + '/core/libs.gradle'
			apply from: rootDir.path + '/core/repo.gradle'
			apply from: rootDir.path + '/core/license.gradle'

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

				// create src-gen directory so that it's picked up as source folder
				file("${buildDir}/src-gen").mkdirs()

				// seperate classpath for main & test sources
				classpath {
					defaultOutputDir = file("bin/main")	
					file {
						whenMerged {  cp -> 
							cp.entries.findAll { it instanceof SourceFolder && it.path.startsWith("src/main/") }*.output = "bin/main" 
							cp.entries.findAll { it instanceof SourceFolder && it.path.startsWith("src/test/") }*.output = "bin/test" 
						}
					}
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
								srcDir "${buildDir}/src-gen"
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
									srcDir "${buildDir}/src-gen"
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
            }
		}
	}
}
