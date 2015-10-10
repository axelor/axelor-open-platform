/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec
import org.gradle.internal.os.OperatingSystem

import com.axelor.gradle.tasks.GenerateCode
import com.axelor.gradle.tasks.VersionTask

class AppPlugin extends AbstractPlugin {
    
	void apply(Project project) {

		project.configure(project) {

			apply plugin: 'war'
			apply plugin: 'com.bmuschko.tomcat'

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
				providedCompile	libs.javax_servlet_jsp

				def tomcatVersion = '7.0.64'
				tomcat "org.apache.tomcat.embed:tomcat-embed-core:${tomcatVersion}",
					   "org.apache.tomcat.embed:tomcat-embed-logging-juli:${tomcatVersion}",
					   "org.apache.tomcat.embed:tomcat-embed-jasper:${tomcatVersion}"
			}

			allprojects {
				configurations {
					axelorCore
					axelorCore.transitive = false
					compile.exclude group: 'c3p0', module: 'c3p0'
				}
				dependencies {
					axelorCore "com.axelor:axelor-common:${sdkVersion}"
					axelorCore "com.axelor:axelor-core:${sdkVersion}"
					axelorCore "com.axelor:axelor-web:${sdkVersion}"
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

			task("gulp", type: Exec, dependsOn: 'npm') {
				description "Run gulp command to build web resource bundles."
				group "Axelor web"
				def command = "gulp"
				if (OperatingSystem.current().isWindows()) {
					command = "gulp.cmd"
				}
				workingDir "${buildDir}/webapp"
				commandLine = [command]
			}

			task("init", dependsOn: "classes", type: JavaExec) {
				description "Initialize application database."
				group "Axelor web"
				main = "com.axelor.app.internal.AppCli"
				classpath = sourceSets.main.runtimeClasspath
				if (project.properties.update) args "-u" else args "-i"
				if (project.properties.modules) args "-m " + project.properties.modules
				jvmArgs "-Daxelor.config=" + System.getProperty('axelor.config')
			}

			task("migrate", dependsOn: "classes", type: JavaExec) {
				description "Run database migration scripts."
				group "Axelor"
				main = "com.axelor.app.internal.AppCli"
				classpath = sourceSets.main.runtimeClasspath
				args "-M"
				if (project.properties.verbose) args "--verbose"
				jvmArgs "-Daxelor.config=" + System.getProperty('axelor.config')
			}

			compileJava.dependsOn "generateCode"

			war.dependsOn "copyWebapp"
			war.from "${buildDir}/webapp"
			war.exclude "node_modules", "gulpfile.js", "package.json"
			war.duplicatesStrategy = "EXCLUDE"

			tomcatRun.dependsOn "copyWebapp"
			tomcatRun.webAppSourceDirectory = file("${buildDir}/webapp")
        }
    }
}
