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
		this.name = node.@name.toString()
		this.fields = node.@using.toString().trim().split(/\s*,\s*/)
		this.filter = node.@filter.toString();
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

	String getCode() {
		
		def query = []
		def params = []
		def args = []
		
		for(String field : fields) {
			def n = Utils.firstLower(field);
			def p = entity.getField(n);
			if (!p) return ""
			args += n
			query += "self.${n} = :${n}"
			params += p.type + " " + n
		}

		query = filter.trim().length() == 0 ? query.join(" AND ") : filter
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
