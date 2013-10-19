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
package com.axelor.tools.x2j.pojo

import groovy.util.slurpersupport.NodeChild

import com.axelor.tools.x2j.Utils
import com.google.common.base.CaseFormat

class Entity {

	String name

	String table

	String module

	String namespace

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

	private ImportManager importManager

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
		propertyMap = [:]
		constraints = []
		indexes = []
		finders = []

		if (!baseClass) {
			if (node.@logUpdates != "false") {
				baseClass = "com.axelor.auth.db.AuditableModel"
			} else {
				baseClass = "com.axelor.db.Model"
			}
			properties.add(Property.idProperty(this));
		} else {
			if (!strategy || strategy == 'SINGLE') {
				table = null
			}
			hasExtends = true
			importType("com.axelor.db.internal.EntityHelper")
		}

		node."*".each {
			if (it.name() ==  "unique-constraint") {
				return constraints += new Constraint(this, it)
			}
			if (it.name() == "index") {
				return indexes += new Index(this, it)
			}
			if (it.name() ==  "finder-method") {
				return finders += new Finder(this, it)
			}
			Property field = new Property(this, it)
			properties += field
			propertyMap[field.name] = field
			if (field.isVirtual() && !field.isTransient()) {
				dynamicUpdate = true
			}
		}

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
			all += it.fields.collect {
				def n = it
				properties.find {
					it.name == n
				}
			}.flatten().findAll { it != null }
		}

		return all.unique()
	}

	String getEqualsCode() {

		if (hasExtends) {
			return "return EntityHelper.equals(this, obj, $hashAll);"
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
			return "return EntityHelper.hashCode(this, $hashAll);"
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
			if (p.virtual || !p.simple || p.name == "id" || p.name == "version") continue
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

		def a2 = new Annotation(this, "org.hibernate.annotations.Table", false).add("appliesTo", this.table)
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
