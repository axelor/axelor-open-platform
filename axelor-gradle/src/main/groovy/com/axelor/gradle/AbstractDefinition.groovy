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

import org.gradle.api.Project

abstract class AbstractDefinition {
    
	String name
	String title
	String description
    String adkVersion

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

	def adkVersion(String adkVersion) {
		this.adkVersion = adkVersion
	}

	def module(String module) {
		modules << module
    }
}

