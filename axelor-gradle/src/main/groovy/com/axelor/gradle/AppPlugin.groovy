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

import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.wrapper.Wrapper

import com.axelor.gradle.tasks.GenerateCode

class AppPlugin extends AbstractPlugin {
    
	void apply(Project project) {

		project.configure(project) {

			apply plugin: 'war'
			apply plugin: 'tomcat'

            def definition = extensions.create("application", AppDefinition)

			applyCommon(project, definition)

			dependencies {

				testCompile "com.axelor:axelor-test:${sdkVersion}"
				compile 	"com.axelor:axelor-common:${sdkVersion}"
				compile		"com.axelor:axelor-core:${sdkVersion}"
				compile		"com.axelor:axelor-web:${sdkVersion}"
				
				providedCompile	libs.javax_servlet

				def tomcatVersion = '7.0.54'
				tomcat "org.apache.tomcat.embed:tomcat-embed-core:${tomcatVersion}",
					   "org.apache.tomcat.embed:tomcat-embed-logging-juli:${tomcatVersion}"
				tomcat("org.apache.tomcat.embed:tomcat-embed-jasper:${tomcatVersion}") {
					exclude group: 'org.eclipse.jdt.core.compiler', module: 'ecj'
				}
			}
			
            afterEvaluate {
				
				def self = owner
				owner.version definition.version
				owner.subprojects {
					version definition.version
					afterEvaluate {
						try {
							self.tasks.generateCode.dependsOn tasks.generateCode
						} catch (Exception e){}
					}
				}
            }

			task("generateCode", type: GenerateCode) << {
				expandAll()
			}

			compileJava.dependsOn "generateCode"

			def webappDir = "${rootProject.buildDir}/webapp"

			// copy webapp to root build dir
			task("copyWebapp") {
				war.project.configurations.runtime.each {
					if (it.name.startsWith("axelor-web")) {
						def fileList = war.project.zipTree(it)
						copy {
							from fileList.matching { include "webapp/**/*" }
							into file(webappDir).parentFile
						}
					}
				}
				copy {
					from files(["${projectDir}/src/main/webapp"])
					into webappDir
				}
			}

			war.dependsOn "copyWebapp"
			war.from webappDir
			war.duplicatesStrategy = "EXCLUDE"

			tomcatRun.dependsOn "copyWebapp"
			tomcatRun.webAppSourceDirectory = file(webappDir)

			eclipse {
				
				//XXX: running inside eclipse requires commons logging
				dependencies {
					compile	libs.commons_logging
				}

				wtp {
					component {
						resource sourcePath: "../axelor-platform/axelor-web/src/main/webapp", deployPath: "/"
						resource sourcePath: "src/main/webapp", deployPath: "/"
					}
				}
			}
        }
    }
}
