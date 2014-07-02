package com.axelor.gradle

import org.gradle.api.Project

abstract class AbstractDefinition {
    
	String name
	String title
	String description
    
	List<String> modules = []

	def name(String name) {
        this.name = name
    }

    def title(String title) {
        this.title = title
    }

	def description(String description) {
        this.description = description
    }

	def module(String module) {
		modules << module
    }
}

