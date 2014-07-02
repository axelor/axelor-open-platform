package com.axelor.gradle

import org.gradle.api.Project

class ModuleDefinition extends AbstractDefinition {

	Boolean removable

	Boolean removable(Boolean removable) {
		this.removable = removable
	}
}
