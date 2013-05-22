package com.axelor.tool.x2j.pojo

import groovy.util.slurpersupport.GPathResult;
import groovy.util.slurpersupport.NodeChild

import com.axelor.tool.x2j.Utils
import com.google.common.base.CaseFormat

class Entity {

	String name

	String table
	
	String module

	String namespace
	
	String baseClass

	boolean sequential
	
	boolean groovy
	
	boolean hashAll
	
	String cachable
	
	String documentation;

	List<Property> properties
	
	List<Constraint> constraints
	
	List<String> finders
	
	Map<String, Property> propertyMap

	private ImportManager importManager
	
	Entity(NodeChild node) {
		name = node.@name
		table = node.@table
		namespace = node.parent().module."@package"
		module = node.parent().module.@name
		
		sequential = node.@sequential == "true"
		if(node.@sequential == ""
			&& System.getProperty("generate.unique.id") == "true") {
			sequential = true
		}
		
		groovy = node.@lang == "groovy"
		hashAll = node.@hashAll == "true"
		cachable = node.@cachable
		baseClass = "com.axelor.db.Model"
		documentation = findDocs(node)
		
		if (!name) {
			throw new IllegalArgumentException("Entity name not given.")
		}

		if (!table) {
			table = module.toUpperCase() + "_" + CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name)
		}
		
		if (!namespace) {
			namespace = "com.axelor.${module}.db"
		}
		
		importManager = new ImportManager(namespace, groovy)
		
		importType("com.axelor.db.JPA")
		importType("com.axelor.db.Query")
		
		properties = []
		constraints = []
		finders = []
		
		node."*".each {
			if (it.name() == "unique-constraint") {
				constraints += new Constraint(this, it)
			} else if (it.name() == "finder-method") {
				finders += new Finder(this, it)
			} else {
				properties += new Property(this, it)
			}
		}
		
		Property idp = Property.idProperty(this)
		properties = [idp] + properties

		propertyMap = [:]
		properties.each {
			propertyMap[it.name] = it
		}
		
		if (node.@logUpdates != "false") {
			baseClass = "com.axelor.auth.db.AuditableModel"
		}
	}
	
	List<Property> getFields() {
		properties
	}

	String getFile() {
		namespace.replace(".", "/") + "/" + name + "." + (groovy ? "groovy" : "java")
	}
	
	String getBaseClass() {
		return importType(baseClass)
	}

	String findDocs(parent) {
		def children = parent.getAt(0).children
		for (child in children) {
			if (!(child instanceof groovy.util.slurpersupport.Node)) {
				return child
			}
		}
	}
	
	String getDocumentation() {
		String text = Utils.stripCode(documentation, "\n * ")
		if (text == "") {
			return ""
		}
		return """
/**
 * """ + text + """
 */"""
	}
	
	Property getField(String name) {
		return propertyMap[name]
	}
	
	String getCtorCode() {
		def lines = []
		
		lines += "public ${name}() {"
		lines += "}\n"
		
		def fields = properties.findAll { it.isInitParam() }
		if (fields.empty) {
			fields = properties.findAll { it.name =~ /code|name/ }
		}
		if (!fields.empty) {
			def args = fields.collect { Property p -> "$p.type $p.name" }
			lines += "public ${name}(${args.join(', ')}) {"
			fields.each { Property p ->
				lines += "\tthis.${p.name} = ${p.name};"
			}
			lines += "}\n"
		}
		
		return "\t" + Utils.stripCode(lines.join("\n"), "\n\t");
	}
	
	private List<Property> getHashables() {
		
		if (hashAll) {
			return properties.findAll { p ->
				p.getAttribute("hashKey") != "false" && !p.virtual && p.simple && !(p.name =~ /id|version/)
			}
		}
		def all = properties.findAll { p ->
			p.hashKey && !p.virtual && p.simple && !(p.name =~ /id|version/)
		}
		
		constraints.each {
			all += it.columns.collect {
				def n = it
				properties.find {
					it.name == n
				}
			}.flatten().findAll { it != null }
		}
		
		return all.unique()
	}
	
	String getEqualsCode() {
		def hashables = getHashables()
		def code = ["if (obj == null) return false;"];
		
		importType("com.google.common.base.Objects");

		if (groovy) {
			code += "if (this.is(obj)) return true;"
		} else {
			code += "if (this == obj) return true;"
		}
		code += "if (!(obj instanceof ${name})) return false;"
		code += ""
		code += "${name} other = (${name}) obj;"
		code += "if (this.getId() != null && other.getId() != null) {"
		code += "\treturn Objects.equal(this.getId(), other.getId());"
		code += "}"
		if (!hashables.empty) {
			code += ""
			code += getHashables().collect { p -> "if (!Objects.equal(${p.getter}(), other.${p.getter}())) return false;"}
		}
		code += ""
		code += hashables.empty ? "return false;" : "return true;"
		return code.join("\n\t\t")
	}
	
	String getHashCodeCode() {
		importType("com.google.common.base.Objects");
		def data = getHashables()collect { "this.${it.getter}()" }.join(", ")
		if (data.size()) {
			def hash = name.hashCode()
			return "return Objects.hashCode(${hash}, ${data});"
		}
		return "return super.hashCode();"
	}
	
	String getToStringCode() {
		importType("com.google.common.base.Objects.ToStringHelper")
		
		def code = []
		
		code += "ToStringHelper tsh = Objects.toStringHelper(this);\n"
		code += "tsh.add(\"id\", this.getId());"
		int count = 0;
		for(Property p : properties) {
			if (p.virtual || !p.simple || p.name == "id" || p.name == "version") continue
			code += "tsh.add(\"${p.name}\", this.${p.getter}());"
			if (count++ == 10) break;
		}
		return code.join("\n\t\t") + "\n\n\t\treturn tsh.omitNullValues().toString();"
	}
	
	String importType(String fqn) {
		return importManager.importType(fqn)
	}
	
	List<String> getImports() {
		return importManager.getImports()
	}
	
	List<String> getImportStatements() {
		return importManager.getImportStatements()
	}
	
	List<Annotation> getAnnotations() {
		[
			new Annotation(this, "javax.persistence.Entity", true),
			$cachable(),
			$table()
		]
		.grep { it != null }.flatten()
		.grep { Annotation a -> !a.empty }
	}
	
	List<Finder> getFinderMethods() {
		def all = finders.collect()
		def hasCodeFinder = false
		def hasNameFinder = false
		
		all.each { Finder f ->
			if (f.name == "findByName") hasNameFinder = true
			if (f.name == "findByCode") hasCodeFinder = true
		}
		
		if (!hasNameFinder && propertyMap['name']) all.add(0, new Finder(this, "name"));
		if (!hasCodeFinder && propertyMap['code']) all.add(0, new Finder(this, "code"));

		return all
	}
	
	Annotation $table() {
		def annotation = new Annotation(this, "javax.persistence.Table", false).add("name", this.table)
		if (this.constraints == null || this.constraints.empty)
			return annotation
		
		def constraints = []
		
		this.constraints.each {
			def unique = new Annotation(this, "javax.persistence.UniqueConstraint", false)
			if (it.name)
				unique.add("name", it.name)
			constraints += unique.add("columnNames", it.columns, true)
		}

		if (! constraints.empty)
			annotation.add("uniqueConstraints", constraints, false)
		
		return annotation
	}
	
	Annotation $cachable() {
		if (cachable == "true") {
			return new Annotation(this, "javax.persistence.Cacheable", true);
		}
		if (cachable == "false") {
			return new Annotation(this, "javax.persistence.Cacheable", false).add("false", false);
		}
		return null
	}
}
