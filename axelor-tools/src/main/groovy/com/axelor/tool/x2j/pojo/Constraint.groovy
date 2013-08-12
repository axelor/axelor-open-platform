package com.axelor.tool.x2j.pojo

import groovy.util.slurpersupport.NodeChild

import com.google.common.base.CaseFormat

class Constraint {

	String name

	List<String> columns

	List<String> fields

	Entity entity

	Constraint(Entity entity, NodeChild node) {
		this.entity = entity
		this.name = node.@name
		this.columns = []
		this.fields = []

		((node.@columns as String).trim().split(/\,/) as List).each { String column ->
			String field = column
			if (field.empty) return
			if (field.contains("_")) {
				field = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, field)
			}

			Property p = entity.getField(field)

			fields += field
			columns += p ? this.column(p) : column
		}
	}

	private String column(Property p) {
		if (p.attrs.column) return p.attrs.column
		if (p.isReference()) return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, p.name)
		return p.name
	}

	List<String> getColumns() {
		return this.columns
	}

	List<String> getFields() {
		return this.fields
	}
}