/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
package com.bmuschko.gradle.tomcat.embedded

class Tomcat85Server extends Tomcat8xServer {

	private static final Set<String> IGNORE_PATTERNS = [
		"ecj-*.jar",
		"junit-*.jar",
		"logback*.jar",
		"*slf4j*.jar",
		"tomcat-embed*.jar"
	]

	private def loggerConfig

	private def createLoggerConfig() {
		def props = new Properties()
		try {
			def config = System.getProperty("axelor.config", "src/main/resources/application.properties")
			def stream = new FileInputStream(new File(config))
			try {
				props.load(stream)
			} finally {
				stream.close()
			}
		} catch (IOException e) {}

		return loadClass('com.axelor.common.logging.LoggerConfiguration').newInstance(props)
	}

	@Override
	String getServerClassName() {
		if (loggerConfig == null) {
			loggerConfig = createLoggerConfig()
			loggerConfig.install()
		}
		return 'org.apache.catalina.startup.Tomcat'
	}

	@Override
	public void stop() {
		try {
			super.stop();
		} finally {
			loggerConfig.uninstall()
			loggerConfig = null
		}
	}

	@Override
	void createContext(String fullContextPath, String webAppPath) {
		super.createContext(fullContextPath, webAppPath)
		context.resources.cacheMaxSize = 100 * 1024;
	}

	@Override
	void addWebappResource(File resource) {
		if (matchName(resource.name)) {
			return
		}

		def mountPath
		def className

		if (resource.directory) {
			mountPath = '/WEB-INF/classes'
			className = 'org.apache.catalina.webresources.DirResourceSet'
		} else {
			mountPath = '/WEB-INF/lib/' + resource.name
			className = 'org.apache.catalina.webresources.FileResourceSet'
		}

		def resourceSet = loadClass(className).newInstance(context.resources, mountPath, resource.absolutePath, '/')
		context.resources.addPreResources(resourceSet)
	}

	private boolean matchName(String name) {
		def klass = loadClass('org.apache.tomcat.util.file.Matcher')
		return klass.matchName(IGNORE_PATTERNS, name)
	}
}
