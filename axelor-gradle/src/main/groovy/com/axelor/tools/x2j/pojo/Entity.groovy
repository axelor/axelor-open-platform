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
package com.axelor.tools.x2j.pojo

import groovy.util.slurpersupport.NodeChild

import com.axelor.common.Inflector
import com.axelor.tools.x2j.Utils

class Entity {

	String name

	String table

	String module

	String namespace
	
	transient long lastModified

	String baseClass

	String strategy

	boolean sequential

	boolean groovy

	boolean dynamicUpdate

	boolean hashAll

	boolean hasExtends

	String cachable

	String documentation

	List<Property> properties

	List<Constraint> constraints

	List<Index> indexes

	List<String> finders

	Map<String, Property> propertyMap

	private Repository repository

	private ImportManager importManager
	
	private String extraImports
	
	private String extraCode

	Entity(NodeChild node) {
		name = node.@name
		table = node.@table
		namespace = node.parent().module."@package"
		module = node.parent().module.'@name'

		sequential = !(node.'@sequential' == "false")
		groovy = node.'@lang' == "groovy"
		hashAll = node.'@hashAll' == "true"
		cachable = node.'@cachable'
		baseClass = node.'@extends'
		strategy = node.'@strategy'
		documentation = findDocs(node)

		if (!name) {
			throw new IllegalArgumentException("Entity name not given.")
		}
		
		if (!namespace) {
			namespace = "com.axelor.${module}.db"
		}

		if (!table) {
			table = namespace.replaceAll(/\.db$/, "")
			table = table.substring(table.lastIndexOf('.') + 1)
			table = Inflector.getInstance().underscore(table + '_' + name).toUpperCase()
		}

		importManager = new ImportManager(namespace, groovy)

		if (node.@repository != "none") {
			repository = new Repository(this)
			repository.concrete = node.@repository != "abstract"
		}
		
		if (!this.pojo) {
			importType("javax.persistence.EntityManager")
			importType("com.axelor.db.Model")
			importType("com.axelor.db.JPA")
			importType("com.axelor.db.Query")
		}

		properties = []
		propertyMap = [:]
		constraints = []
		indexes = []
		finders = []
		extraCode = null

		if (!baseClass) {
			if (node.@logUpdates != "false") {
				baseClass = "com.axelor.auth.db.AuditableModel"
			} else {
				baseClass = "com.axelor.db.Model"
			}
			propertyMap.put("id", Property.idProperty(this));
			properties.add(propertyMap.get("id"));
		} else {
			if (!strategy || strategy == 'SINGLE') {
				table = null
			}
			hasExtends = true
			importType("com.axelor.db.internal.EntityHelper")
		}

		node."*".each {
			switch (it.name()) {
			case "index":
				indexes += new Index(this, it)
				break
			case "unique-constraint":
				constraints += new Constraint(this, it)
				break
			case "finder-method":
				finders += new Finder(this, it)
				break
			case "extra-imports":
				extraImports = it.text()
				break
			case "extra-code":
				extraCode = it.text()
				break
			default:
				Property field = new Property(this, it)
				properties += field
				propertyMap[field.name] = field
				if (field.isVirtual() && !field.isTransient()) {
					dynamicUpdate = true
				}
			}
		}
	}
	
	Repository getRepository() {
		return this.repository
	}

	boolean isPojo() {
		return System.getProperty("codegen.pojo", "true") == "true"
	}

	void merge(Entity other) {
		
		for (Property prop : other.properties) {
			if (!propertyMap.containsKey(prop.name)) {
				prop.entity = this
				properties.add(prop)
				propertyMap[prop.name] = prop;
			}
		}
		
		indexes.addAll(other.indexes)
		constraints.addAll(other.constraints)
		finders.addAll(other.finders)
		
		extraImports = extraImports?:"" + other.extraImports?:""
		extraCode = extraCode?:"" + other.extraCode?:""
	}

	boolean addField(Property field) {
		for (Property current : properties) {
			if (current.name == field.name) {
				return false
			}
		}
		field.entity = this
		properties.add(field);
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

		if (!hasExtends) {
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
		}

		return "\t" + Utils.stripCode(lines.join("\n"), "\n\t")
	}
	
	String getExtraCode() {
		if (!extraCode || extraCode.trim().isEmpty()) return "";
		return "\n\t" + Utils.stripCode(extraCode, "\n\t")
	}
	
	String getExtraImports() {
		if (!extraImports || extraImports.trim().isEmpty()) return "";
		return "\n" + Utils.stripCode(extraImports, "\n") + "\n"
	}

	private List<Property> getHashables() {
		return properties.findAll { p -> p.hashKey }
	}

	String getEqualsCode() {

		if (hasExtends) {
			return "return EntityHelper.equals(this, obj);"
		}

		def hashables = getHashables()
		def code = ["if (obj == null) return false;"]

		importType("com.google.common.base.Objects")

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
		if (hasExtends) {
			return "return EntityHelper.hashCode(this);"
		}
		importType("com.google.common.base.Objects")
		def data = getHashables()collect { "this.${it.getter}()" }.join(", ")
		if (data.size()) {
			def hash = name.hashCode()
			return "return Objects.hashCode(${hash}, ${data});"
		}
		return "return super.hashCode();"
	}

	String getToStringCode() {
		if (hasExtends) {
			return "return EntityHelper.toString(this);"
		}
		importType("com.google.common.base.Objects.ToStringHelper")

		def code = []

		code += "ToStringHelper tsh = Objects.toStringHelper(this);\n"
		code += "tsh.add(\"id\", this.getId());"
		int count = 0
		for(Property p : properties) {
			if (p.virtual || p.password || !p.simple || p.name == "id" || p.name == "version") continue
			code += "tsh.add(\"${p.name}\", this.${p.getter}());"
			if (count++ == 10) break
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

		def all = [new Annotation(this, "javax.persistence.Entity", true), $cachable()]

		if (dynamicUpdate) {
			all += new Annotation(this, "org.hibernate.annotations.DynamicInsert", true)
			all += new Annotation(this, "org.hibernate.annotations.DynamicUpdate", true)
		}

		all += $table()
		all += $strategy()

		return all.grep { it != null }.flatten()
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

		if (!hasNameFinder && propertyMap['name']) all.add(0, new Finder(this, "name"))
		if (!hasCodeFinder && propertyMap['code']) all.add(0, new Finder(this, "code"))

		return all
	}

	List<Annotation> $table() {

		if (!table) return []

		def constraints = this.constraints.collect {
			def idx = new Annotation(this, "javax.persistence.UniqueConstraint", false)
			if (it.name) {
				idx.add("name", it.name)
			}
			return idx.add("columnNames", it.columns, true)
		}

		def indexes = this.indexes.collect {
			def idx = new Annotation(this, "org.hibernate.annotations.Index", false)
			if (it.name) {
				idx.add("name", it.name)
			}
			return idx.add("columnNames", it.columns, true)
		}

		def a1 = new Annotation(this, "javax.persistence.Table", false).add("name", this.table)
		if (!constraints.empty)
			a1.add("uniqueConstraints", constraints, false)

		if (indexes.empty)
			return [a1]

		def a2 = new Annotation(this, "org.hibernate.annotations.Table", false).add("appliesTo", this.table.toLowerCase())
		a2.add("indexes", indexes, false)

		return [a1, a2]
	}

	Annotation $cachable() {
		if (cachable == "true") {
			return new Annotation(this, "javax.persistence.Cacheable", true)
		}
		if (cachable == "false") {
			return new Annotation(this, "javax.persistence.Cacheable", false).add("false", false)
		}
		return null
	}

	Annotation $strategy() {
		if (!strategy) return null
		String type = "SINGLE_TABLE"
		if (strategy == "JOINED") type = "JOINED"
		if (strategy == "CLASS") type = "TABLE_PER_CLASS"

		return new Annotation(this, "javax.persistence.Inheritance")
				.add("strategy", "javax.persistence.InheritanceType.${type}", false)
	}

	@Override
	String toString() {
		def names = fields.collect { it.name }
		return "Entity(name: $name, fields: $names)"
	}
}
