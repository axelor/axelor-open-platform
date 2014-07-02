package com.axelor.gradle

import org.gradle.api.Project

class AppDefinition extends AbstractDefinition {

	String version
    
	def version(String version) {
        this.version = version
    }
}
