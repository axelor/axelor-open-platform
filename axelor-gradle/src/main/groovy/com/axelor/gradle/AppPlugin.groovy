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

import java.util.regex.Pattern

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec
import org.gradle.internal.os.OperatingSystem

import com.axelor.common.VersionUtils
import com.axelor.gradle.tasks.GenerateCode
import com.axelor.gradle.tasks.VersionTask

class AppPlugin extends AbstractPlugin {
    
	void apply(Project project) {

		project.configure(project) {

			apply plugin: 'war'
			apply plugin: 'tomcat'

            def definition = extensions.create("application", AppDefinition)

			afterEvaluate {
				checkVersion(project, definition)
			}

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

			allprojects {
				configurations {
					axelorCore
					axelorCore.transitive = false
				}
				dependencies {
					axelorCore "com.axelor:axelor-common:${sdkVersion}"
					axelorCore "com.axelor:axelor-core:${sdkVersion}"
					axelorCore "com.axelor:axelor-web:${sdkVersion}"
					axelorCore "com.axelor:axelor-wkf:${sdkVersion}"
					axelorCore "com.axelor:axelor-test:${sdkVersion}"
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
					project.subprojects { sub ->
						if (sub.plugins.hasPlugin('axelor-module')) {
							linkCoreProjects(sub)
						}
					}
				}
            }

			task('updateVersion', type: VersionTask) {
				description "Update version text in source files."
				group "Axelor"
				processFiles = fileTree(projectDir) {
					include '**/resources/**/*.xml'
					include '**/data/**/*config.xml'
				}
			}

			task("generateCode", type: GenerateCode) << {
				description "Generate code for domain models from xml definitions."
				group "Axelor"
				expandAll()
			}

			// copy webapp to root build dir
			task("copyWebapp", type: Copy) {
				destinationDir = file(buildDir)
				from zipTree(configurations.axelorCore.find { it.name.startsWith('axelor-web') }).matching { include 'webapp/**/*' }
				into("webapp") {
					from "src/main/webapp"
				}
			}

			task("npm", type: Exec, dependsOn: 'copyWebapp') {
				description "Run 'npm install' command to install npm packages."
				group "Axelor web"
				workingDir "${buildDir}/webapp"
				commandLine = ["npm", "install"]
			}

			task("grunt", type: Exec, dependsOn: 'npm') {
				description "Run grunt command to build web resources."
				group "Axelor web"
				def command = "grunt"
				if (OperatingSystem.current().isWindows()) {
					command = "grunt.cmd"
				}
				workingDir "${buildDir}/webapp"
				commandLine = [command]
			}

			task("init", dependsOn: "classes", type: JavaExec) {
				description "Initialize application database."
				group "Axelor web"
				main = "com.axelor.app.internal.AppInitCli"
				classpath = sourceSets.main.runtimeClasspath
				if (project.properties.update) args "-u" else args "-i"
				if (project.properties.modules) args "-m " + project.properties.modules
				jvmArgs "-Daxelor.config=" + System.getProperty('axelor.config')
			}

			compileJava.dependsOn "generateCode"

			war.dependsOn "copyWebapp"
			war.from "${buildDir}/webapp"
			war.exclude "node_modules", "Gruntfile.js", "package.json"
			war.duplicatesStrategy = "EXCLUDE"

			tomcatRun.dependsOn "copyWebapp"
			tomcatRun.webAppSourceDirectory = file("${buildDir}/webapp")

			// add eclipse launcher
			eclipseLaunchers(project)
        }
    }

	private Pattern namePattern = ~/^(axelor-(?:common|core|web|wkf))-/

	private void checkVersion(Project project, AppDefinition definition) {
		if (!definition.adkVersion || VersionUtils.version.matches(definition.adkVersion)) {
			return;
		}
		throw new GradleException("ADK version mismatch, requires: " + definition.adkVersion);
	}

	private List<String> findCoreModules(Project project) {
		def all = []
		project.configurations.runtime.each { File lib ->
			def m = namePattern.matcher(lib.name)
			if (m.find()) {
				all += [m.group(1)]
			}
		}
		return all
	}

	private void linkCoreProjects(Project project) {

		def linked = findCoreModules(project)
		def wtpLinked = linked - ['axelor-test']

		project.eclipse.classpath {
			minusConfigurations += [project.configurations.axelorCore]
		}
		project.eclipse.classpath.file {
			withXml {
				def node = it.asNode()
				def ref = node.find { it.@path == "org.eclipse.jst.j2ee.internal.web.container" }
				if (ref) {
					ref.plus {
						linked.collect { name -> classpathentry(kind: 'src', path: "/${name}", exported: 'true')}
					}
				} else {
					linked.each { name -> node.appendNode('classpathentry', [kind: 'src', path: "/${name}", exported: 'true']) }
				}
			}
		}

		if (!project.plugins.hasPlugin("war")) {
			return
		}

		project.eclipse.wtp.component {
			minusConfigurations += [project.configurations.axelorCore]
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
			linkedResource name: 'axelor-webapp', type: '2', location: '${WORKSPACE_LOC}/axelor-development-kit/axelor-web/src/main/webapp'
		}
	}

	private void eclipseLaunchers(Project project) {
		project.tasks.eclipse.doLast {
			def home = System.getenv("AXELOR_HOME")
			def launcher = project.file(".settings/Generate Code (${project.name}).launch")
			launcher.text = """<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<launchConfiguration type="org.eclipse.ui.externaltools.ProgramLaunchConfigurationType">
<stringAttribute key="org.eclipse.debug.core.ATTR_REFRESH_SCOPE" value="\${workspace}"/>
<mapAttribute key="org.eclipse.debug.core.environmentVariables">
<mapEntry key="AXELOR_HOME" value="${home}"/>
</mapAttribute>
<stringAttribute key="org.eclipse.ui.externaltools.ATTR_LAUNCH_CONFIGURATION_BUILD_SCOPE" value="\${none}"/>
<stringAttribute key="org.eclipse.ui.externaltools.ATTR_LOCATION" value="${project.projectDir}/gradlew"/>
<stringAttribute key="org.eclipse.ui.externaltools.ATTR_RUN_BUILD_KINDS" value="full,"/>
<stringAttribute key="org.eclipse.ui.externaltools.ATTR_TOOL_ARGUMENTS" value="generateCode"/>
<booleanAttribute key="org.eclipse.ui.externaltools.ATTR_TRIGGERS_CONFIGURED" value="true"/>
<stringAttribute key="org.eclipse.ui.externaltools.ATTR_WORKING_DIRECTORY" value="${project.projectDir}"/>
</launchConfiguration>
"""
		}
	}
}
