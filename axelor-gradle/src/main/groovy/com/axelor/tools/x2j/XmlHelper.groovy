/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
import com.axelor.tools.x2j.pojo.EnumType

class XmlHelper {

	/**
	 * Parse the given input xml and return {@link Entity} mapping
	 * to each entity elements.
	 *
	 * @param input the input file
	 * @return list of entity mapping
	 */
	public static List<Entity> entities(File input) {
		return new XmlSlurper().parse(input).'entity'.collect {
			try {
				return new Entity(it)
			} catch (Exception e) {
				throw new IllegalArgumentException("Error processing: ${input}", e)
			}
		}
	}

	/**
	 * Parse the given input xml and return entity names defined in the file.
	 *
	 * @param input the input file
	 * @return list of entity names
	 */
	public static Set<String> findEntityNames(File input) {
		return new XmlSlurper().parse(input).'entity'.collect {
			return (String) it.@name
		}
	}
	
	/**
	 * Parse the given input xml and return {@link EnumType} mapping
	 * to each entity elements.
	 *
	 * @param input the input file
	 * @return list of entity mapping
	 */
	public static List<EnumType> enums(File input) {
		return new XmlSlurper().parse(input).'enum'.collect {
			return new EnumType(it)
		}
	}

	/**
	 * Parse the given input xml and return entity names defined in the file.
	 *
	 * @param input the input file
	 * @return list of entity names
	 */
	public static Set<String> findEnumNames(File input) {
		return new XmlSlurper().parse(input).'enum'.collect {
			return (String) it.@name
		}
	}
}
