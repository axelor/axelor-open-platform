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
package com.axelor.tools.x2j

import com.axelor.tools.x2j.pojo.Entity

class XmlHelper {

	/**
	 * Read return the list of modules from the given pom.xml
	 *
	 * @param input the input file
	 * @return list of all the modules found in the xml
	 */
	public static List<String> modules(File input) {
		return new XmlSlurper().parse(input).'**'.findAll { it.name() == "module" }.collect { it.text() }
	}

	/**
	 * Parse the given input xml and return {@link Entity} mapping
	 * to each entity elements.
	 *
	 * @param input the input file
	 * @return list of entity mapping
	 */
	public static  List<Entity> entities(File input) {
		return new XmlSlurper().parse(input).'entity'.collect {
			return new Entity(it)
		}
	}

	public static String version(File input) {
		def version = new XmlSlurper().parse(input).'**'.find { it.name() == 'version' }
		if (version) {
			return version.text()
		}
		return "1.0.0-SNAPSHOT"
	}
}
