/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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

import groovy.util.slurpersupport.NodeChild

import com.axelor.tools.x2j.Utils

class Finder {

	Entity entity

	String name

	String type

	String filter

	Boolean cacheable

	Boolean flush

	List<String> orderBy

	List<String> fields

	Finder(Entity entity, NodeChild node) {
		this.entity = entity
		this.name = node.@name.toString().trim()
		this.fields = node.@using.toString().trim().split(/\s*,\s*/)
		this.filter = node.@filter.toString().trim();
		this.orderBy = node.@orderBy.toString().trim().split(/\s*,\s*/)

		this.type = entity.name
		if (node.@all == "true") {
			type = "Query<" + type + ">"
		}

		fields = fields.findAll { s -> !s.empty }
		orderBy = orderBy.findAll { s -> !s.empty }

		if (node.@cacheable == "true") cacheable = true
		if (node.@flush == "false") flush = false
	}

	Finder(Entity entity, String field) {
		this.entity = entity
		this.name = "findBy" + Utils.firstUpper(field)
		this.type = entity.name
		this.fields = [field]
		this.filter = ""
	}

	private static final def TYPES = [
		"int"		: "int",
		"long"		: "long",
		"double"	: "double",
		"boolean"	: "boolean",
		"Integer"	: "Integer",
		"Long"		: "Long",
		"Double"	: "Double",
		"Boolean"	: "Boolean",
		"String"	: "String",
		"LocalDate"		: "java.time.LocalDate",
		"LocalTime"		: "java.time.LocalTime",
		"LocalDateTime"	: "java.time.LocalDateTime",
		"ZonedDateTime"	: "java.time.ZonedDateTime",
		"BigDecimal"	: "java.math.BigDecimal"
	]

	String getCode(boolean useStatic) {

		def query = []
		def params = []
		def args = []

		for(String field : fields) {
			def parts = field.split(/\:/)
			def n = Utils.firstLower(field)
			def p
			def t
			if (parts.length > 1) {
				if (filter.empty) return "" // filter must be provided
				t = parts[0]
				n = parts[1]
				if (TYPES[t]) {
					t = entity.importType(TYPES[t])
					t = entity.repository.importType(TYPES[t])
				} else {
					t = entity.importType(parts[0])
					t = entity.repository.importType(parts[0])
				}
			} else {
				p = entity.getField(n)
				if (!p && entity.baseEntity) p = entity.baseEntity.getField(n)
				if (!p) return ""
				t = p.type

				if (p.targetFqn) {
					t = p.targetFqn.indexOf('.') == -1 ? entity.namespace + '.' + t : p.targetFqn
					t = entity.repository.importType(t)
				}
				query += "self.${n} = :${n}"
			}
			n = Utils.firstLower(n);

			args += n
			params += t + " " + n
		}

		entity.repository.importType("com.axelor.db.Query")

		query = filter.empty ? query.join(" AND ") : filter
		params = params.join(", ")

		def lines = []

		if (useStatic) {
			lines += "public static ${type} ${name}(${params}) {"
		} else {
			lines += "public ${type} ${name}(${params}) {"
		}

		lines += "\treturn Query.of(${entity.name}.class)"
		lines += "\t\t\t.filter(\"${query}\")"

		args.each { n ->
			lines += "\t\t\t.bind(\"${n}\", ${n})"
		}

		orderBy.each { n ->
			lines += "\t\t\t.order(\"${n}\")"
		}

		if (cacheable == Boolean.TRUE) {
			lines += "\t\t\t.cacheable()"
		}
		if (flush == Boolean.FALSE) {
			lines += "\t\t\t.autoFlush(false)"
		}
		if (type == entity.name) {
			lines += "\t\t\t.fetchOne();"
		} else {
			lines[lines.size() - 1] = lines.last() + ";"
		}

		lines += "}"

		return "\n\t" + Utils.stripCode(lines.join("\n"), "\n\t") + "\n";
	}

	@Override
	String toString() {
		return "Finder(" + name + ")";
	}
}
