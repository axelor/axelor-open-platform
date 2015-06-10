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
package com.axelor.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.plugins.ide.eclipse.model.SourceFolder

import com.axelor.common.VersionUtils
import com.axelor.gradle.tasks.I18nTask


abstract class AbstractPlugin implements Plugin<Project> {

	String sdkVersion
	
	protected String getSdkVersion() {
		if (sdkVersion) {
			return sdkVersion
		}
		return sdkVersion = VersionUtils.getVersion().version;
	}
	
	protected void checkVersion(Project project, AbstractDefinition definition) {
		if (!definition.adkVersion || VersionUtils.version.matches(definition.adkVersion)) {
			return;
		}
		throw new GradleException(
			String.format("ADK version mismatch, '%s' requires: %s",
				definition.name, definition.adkVersion));
	}

	protected void applyCommon(Project project, AbstractDefinition definition) {

		project.configure(project) {

			apply plugin: 'groovy'
			apply plugin: 'eclipse'
			apply plugin: 'eclipse-wtp'
		
			dependencies {
				compile libs.slf4j
				compile libs.groovy
				testCompile	libs.junit
			}

			task('i18n-extract', type: I18nTask) {
				description "Extract i18n messages from source files."
				group "Axelor i18n"
				update = false
				withContext = project.properties['with.context'] ? true : false
			}
			task('i18n-update', type: I18nTask) {
				description "Update i18 messages from message catalog."
				group "Axelor i18n"
				update = true
			}

			tasks.eclipse.dependsOn "cleanEclipse"

			eclipse {

				// create src-gen directory so that it's picked up as source folder
				file("${buildDir}/src-gen").mkdirs()

				// seperate output for main & test sources
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

				// add module dependency
				definition.modules.each { module ->
					dependencies {
						compile project.project(":${module}")
					}
                }
				
				// add src-gen as source directory
				sourceSets {
					main {
						java {
							srcDir "${buildDir}/src-gen"
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
								srcDirs = ["src/main/java", "src/main/groovy", "${buildDir}/src-gen"]
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
