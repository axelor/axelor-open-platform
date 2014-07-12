package com.axelor.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.artifacts.ProjectDependency
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

				testCompile owner.project(":core:axelor-test")
				compile 	owner.project(":core:axelor-common")
				compile		owner.project(":core:axelor-core")
				compile		owner.project(":core:axelor-web")

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
							self.tasks.copyClasses.dependsOn tasks.generateCode
						} catch (Exception e){}
					}
				}
            }

			// define wrapper task with proper gradle version
			task("wrapper", type: Wrapper) {
				gradleVersion = '2.0'
			}
			
			task("generateCode", type: GenerateCode)

			
			def genCard = [:]
			def genPath = [:]

			ext.classGenerated = { String name, String path, int cardinality ->
				if (cardinality < 2) return
				def n = genCard[name]
				if (n == null || n < cardinality) {
					genCard[name] = cardinality
					genPath[name] = path
				}
			}

			task("copyClasses", dependsOn: "generateCode") << {
				genPath.each {
					def source = it.value
					def output = source.substring(source.indexOf('src-gen') + 8)
					
					output = "${buildDir}/src-gen/${output}"
					
					copy {
						from file(source)
						into file(output).parent
					}
				}
			}
			
			compileJava.dependsOn "copyClasses"
			
			def webappDir = "${rootProject.buildDir}/webapp"

			// copy webapp to root build dir
			task("copyWebapp", type: Copy) {
				from files(["${projectDir}/core/axelor-web/src/main/webapp", "${projectDir}/src/main/webapp"])
				into webappDir
			}

			war.dependsOn "copyWebapp"
			war.from webappDir

			tomcatRun.dependsOn "copyWebapp"
			tomcatRun.webAppSourceDirectory = file(webappDir)

			eclipse {
				
				//XXX: running inside eclipse requires commons logging
				dependencies {
					compile	libs.commons_logging
				}

				wtp {
					component {
						resource sourcePath: "core/axelor-web/src/main/webapp", deployPath: "/"
						resource sourcePath: "src/main/webapp", deployPath: "/"
					}
				}
			}
        }
    }
}
