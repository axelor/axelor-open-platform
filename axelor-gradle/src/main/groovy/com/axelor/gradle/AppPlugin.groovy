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
			
			configurations {
				axelor
			}
			
			dependencies {

				testCompile "com.axelor:axelor-test:${sdkVersion}"
				compile 	"com.axelor:axelor-common:${sdkVersion}"
				compile		"com.axelor:axelor-core:${sdkVersion}"
				compile		"com.axelor:axelor-web:${sdkVersion}"

				axelor 		"com.axelor:axelor-common:${sdkVersion}"
				axelor		"com.axelor:axelor-core:${sdkVersion}"
				axelor		"com.axelor:axelor-web:${sdkVersion}"
				axelor		"com.axelor:axelor-wkf:${sdkVersion}"
				axelor		"com.axelor:axelor-test:${sdkVersion}"

				providedCompile	libs.javax_servlet

				def tomcatVersion = '7.0.54'
				tomcat "org.apache.tomcat.embed:tomcat-embed-core:${tomcatVersion}",
					   "org.apache.tomcat.embed:tomcat-embed-logging-juli:${tomcatVersion}"
				tomcat("org.apache.tomcat.embed:tomcat-embed-jasper:${tomcatVersion}") {
					exclude group: 'org.eclipse.jdt.core.compiler', module: 'ecj'
				}
			}
			
			afterEvaluate {
				project.version = definition.version
				subprojects { p ->
					p.version = definition.version
					try {
						project.tasks.generateCode.dependsOn p.generateCode
					} catch (Exception e) {}
				}

				if (project.hasProperty('linkCoreInEclipse')) {
					linkCoreProjects(project)
				}
            }

			task("generateCode", type: GenerateCode) << {
				expandAll()
			}

			// copy webapp to root build dir
			task("copyWebapp", type: Copy) {
				destinationDir = file(buildDir)
				from zipTree(configurations.axelor.find { it.name.startsWith('axelor-web') }).matching { include 'webapp/**/*' }
				into("webapp") {
					from "src/main/webapp"
				}
			}

			compileJava.dependsOn "generateCode"

			war.dependsOn "copyWebapp"
			war.from "${buildDir}/webapp"
			war.duplicatesStrategy = "EXCLUDE"

			tomcatRun.dependsOn "copyWebapp"
			tomcatRun.webAppSourceDirectory = file("${buildDir}/webapp")

			eclipse {
				
				//XXX: running inside eclipse requires commons logging
				dependencies {
					compile	libs.commons_logging
				}

				wtp {
					component {
						resource sourcePath: "src/main/webapp", deployPath: "/"
					}
				}
			}
        }
    }

	private void linkCoreProjects(Project project) {

		def linked = ['axelor-common', 'axelor-core', 'axelor-web', 'axelor-wkf', 'axelor-test']
		def wtpLinked = linked - ['axelor-test']

		project.eclipse.classpath {
			minusConfigurations += [project.configurations.axelor]
		}
		project.eclipse.wtp.component {
			minusConfigurations += [project.configurations.axelor]
		}
		project.eclipse.classpath.file {
			withXml {
				def node = it.asNode()
				node.find { it.@path == "org.eclipse.jst.j2ee.internal.web.container" }?.plus {
					linked.collect { name ->
						classpathentry(kind: 'src', path: "/${name}", exported: 'true')
					}
				}
			}
		}
		project.eclipse.wtp.component.file {
			withXml {
				def node = it.asNode()['wb-module'][0]
				node.find { it.'@source-path' == "src/main/webapp" }?.replaceNode {
					['wb-resource'('deploy-path': "/", 'source-path': "axelor-webapp"),
					 'wb-resource'('deploy-path': "/", 'source-path': "src/main/webapp")] +
					wtpLinked.collect { name ->
						'dependent-module'('deploy-path': "/WEB-INF/lib", handle: "module:/resource/${name}/${name}") {
							'dependency-type'('uses')
						}
					}
				}
			}
		}
		project.eclipse.project {
			linkedResource name: 'axelor-webapp', type: '2', location: '${WORKSPACE_LOC}/axelor-platform/axelor-web/src/main/webapp'
		}
	}
}
