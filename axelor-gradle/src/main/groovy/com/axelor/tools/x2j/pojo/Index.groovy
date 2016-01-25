/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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
package com.axelor.tools.x2j.pojo

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.slurpersupport.NodeChild

@CompileStatic
class Index {

	String name

	List<String> columns

	List<String> fields

	Entity entity

	Index(Entity entity, NodeChild node) {
		this.entity = entity
		this.name = node.getProperty("@name")
		this.columns = []
		this.fields = []

		(node.getProperty("@columns") as String).trim().split(/\,/).each { String column ->
			def field = column
			if (field.empty) {
				return
			}
			Property property = entity.getField(field)

			fields.add(field)
			columns.add(Index.getColumn(property, column))
		}
	}

	private static String getColumn(Property property, String column) {
		if (property == null) return column
		def col = property.getColumn()
		if (col) return property.getColumn()
		if (property.isReference()) return property.getColumnAuto()
		return column
	}

	List<String> getColumns() {
		return this.columns
	}

	List<String> getFields() {
		return this.fields
	}
}
