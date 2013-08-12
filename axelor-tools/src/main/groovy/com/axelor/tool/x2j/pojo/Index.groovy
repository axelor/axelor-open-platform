package com.axelor.tool.x2j.pojo

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

		((node.getProperty("@columns") as String).trim().split(/\,/) as List).each { String column ->
			def field = column
			if (field.empty) {
				return
			}
			Property property = entity.getField(field)

			fields += field
			columns += Index.getColumn(property, column)
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