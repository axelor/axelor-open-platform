/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
package com.axelor.tool.x2j.pojo

import groovy.util.slurpersupport.NodeChild;

import com.axelor.tool.x2j.Utils;

class Finder {

	Entity entity
	
	String name

	String type
	
	String filter
	
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
		"LocalDate"		: "org.joda.time.LocalDate",
		"LocalTime"		: "org.joda.time.LocalTime",
		"LocalDateTime"	: "org.joda.time.LocalDateTime",
		"DateTime"		: "org.joda.time.DateTime",
		"BigDecimal"	: "java.math.BigDecimal"
	]

	String getCode() {
		
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
				} else {
					return ""
				}
			} else {
				p = entity.getField(n)
				if (!p) return ""
				t = p.type
				query += "self.${n} = :${n}"
			}
			n = Utils.firstLower(n);

			args += n
			params += t + " " + n
		}
		
		query = filter.empty ? query.join(" AND ") : filter
		params = params.join(", ")
		
		def lines = []
		
		lines += "public static ${type} ${name}(${params}) {"
		lines += "\treturn ${entity.name}.all()"
		lines += "\t\t\t.filter(\"${query}\")"
		
		args.each { n ->
			lines += "\t\t\t.bind(\"${n}\", ${n})"
		}
		
		orderBy.each { n ->
			lines += "\t\t\t.order(\"${n}\")"
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
