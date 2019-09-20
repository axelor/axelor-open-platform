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
import org.slf4j.LoggerFactory
import com.axelor.common.Inflector
import com.axelor.tools.x2j.Utils

class Entity {

	private static Set<String> INTERNAL_PACKAGES = [
		'com.axelor.auth.db',
		'com.axelor.meta.db',
		'com.axelor.mail.db',
		'com.axelor.dms.db',
	]

	String name

	String table

	String module

	String namespace

	String repoNamespace

	String tablePrefix

	transient long lastModified

	private String interfaces

	String baseClass

	String strategy

	boolean mappedSuper

	boolean sequential

	boolean groovy

	boolean dynamicUpdate

	boolean hashAll

	boolean hasExtends

	String cacheable

	String documentation

	List<Property> properties

	List<Constraint> constraints

	List<Index> indexes

	List<String> finders

	List<String> listeners

	Map<String, Property> propertyMap

	Property nameField

	private Repository repository

	private ImportManager importManager

	private String extraImports

	private String extraCode

	private Track track

	private boolean modelClass

	Entity baseEntity

	Entity(NodeChild node) {
		name = node.@name
		table = node.@table
		module = node.parent().module.'@name'
		namespace = node.parent().module."@package"
		repoNamespace = node.parent().module."@repo-package"
		tablePrefix = node.parent().module."@table-prefix"
		mappedSuper = node.'@persistable' == 'false'
		sequential = !(node.'@sequential' == "false")
		groovy = node.'@lang' == "groovy"
		hashAll = node.'@hashAll' == "true"
		cacheable = node.'@cacheable'.text().size() > 0 ? node.'@cacheable' : node.'@cachable';
		interfaces = node.'@implements'
		baseClass = node.'@extends'
		strategy = node.'@strategy'
		documentation = findDocs(node)

		if (!name) {
			throw new IllegalArgumentException("Entity name not given.")
		}

		if (!module || !namespace) {
			throw new IllegalArgumentException("Namespace details not given or incomplete.")
		}

		modelClass = namespace == 'com.axelor.db' && name == 'Model'
		if (modelClass) {
			baseClass = null
			mappedSuper = true
		}

		if (!repoNamespace) {
			repoNamespace = "${namespace}.repo"
		}

		if (node.attributes().containsKey('cachable')) {
			LoggerFactory.getLogger(Entity.class).warn("Attribute 'cachable' on ${name} is deprecated, use 'cacheable' instead.")
		}

		if (!tablePrefix) {
			if (namespace.endsWith(".db")) {
				tablePrefix = namespace.replaceAll(/\.db$/, "")
				tablePrefix = tablePrefix.substring(tablePrefix.lastIndexOf('.') + 1) + "_"
			} else {
				tablePrefix = module + "_"
			}
		} else if (!tablePrefix.endsWith("_")) {
			tablePrefix += "_"
		}

		if (!table) {
			table = Inflector.getInstance().underscore(tablePrefix + name).toUpperCase()
		}

		importManager = new ImportManager(namespace, groovy)

		if (!modelClass && node.@repository != "none" && !mappedSuper) {
			repository = new Repository(this)
			repository.concrete = node.@repository != "abstract"
		}

		properties = []
		propertyMap = [:]
		constraints = []
		indexes = []
		finders = []
		listeners = []
		extraCode = null

		if (interfaces) {
			interfaces = interfaces.split(",").collect { importType(it.trim()) }.join(", ")
		}

		if (!modelClass) {
			if (!baseClass) {
				if (node.@logUpdates != "false") {
					baseClass = "com.axelor.auth.db.AuditableModel"
				} else {
					baseClass = "com.axelor.db.Model"
				}
				propertyMap.put("id", Property.idProperty(this));
				properties.add(propertyMap.get("id"));
			} else {
				hasExtends = true
				importType("com.axelor.db.EntityHelper")
			}
		}

		def jsonAttrs = node.'@jsonAttrs'
		def jsonAttrsAdd = jsonAttrs == 'true' || !INTERNAL_PACKAGES.contains(namespace)
		if (jsonAttrs == 'false' || modelClass) {
			jsonAttrsAdd = false
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
				case "track":
					track = new Track(this, it)
					break
				case "entity-listener":
					listeners += it.'@class'.text()
					break
				default:
					Property field = new Property(this, it)
					if (modelClass && !field.simple) {
						throw new IllegalArgumentException("Only simple fields can be added to Model class.")
					}
					properties += field
					propertyMap[field.name] = field
					if (field.isVirtual() && !field.isTransient()) {
						dynamicUpdate = true
					}
					if (field.isNameField()) {
						nameField = field
					}
					if (field.indexable) {
						indexes += field.index
					}
					if (field.json || field.name == 'attrs') {
						jsonAttrsAdd = false
					}
					if (field.name == 'attrs') {
						jsonAttrs = 'false'
					}
			}
		}

		if (jsonAttrsAdd || jsonAttrs == 'true') {
			propertyMap.put("attrs", Property.attrsProperty(this));
			properties.add(propertyMap.get("attrs"));
		}
	}

	Repository getRepository() {
		return this.repository
	}

	private boolean isCompatible(Property existing, Property property) {
		if (existing == null) return true
		if (existing.isCollection() || existing.isTransient() || existing.name == "id") return false;
		if (existing.type != property.type) return false
		if (existing.target != property.target) return false
		if (existing.large && !property.large) return false
		return true
	}

	void merge(Entity other) {

		for (Property prop : other.properties) {
			Property existing = propertyMap.get(prop.name)
			if (isCompatible(existing, prop)) {
				prop.ownEntity = prop.entity
				prop.entity = this
				prop.initParam = false // can't be a constructor param
				if (existing != null) {
					properties.remove(existing)
					if (!existing.ownEntity && existing.initParam) {
						prop.initParam = true // unless existing is a constructor param
					}
				}
				properties.add(prop)
				propertyMap[prop.name] = prop
				if (prop.isNameField()) {
					nameField = prop
				}
			}
		}

		indexes.addAll(other.indexes)
		constraints.addAll(other.constraints)
		finders.addAll(other.finders)
		listeners.addAll(other.listeners)

		if (other.track) {
			if (track == null || other.track.replace) {
				track = other.track.copyFor(this);
			} else {
				track.merge(other.track);
			}
		}

		other.baseEntity = this
		other.repository = this.repository

		extraImports = stripCode(extraImports, "") + stripCode(other.extraImports, "")
		extraCode = stripCode(extraCode, "\n\t") + "\n" + stripCode(other.extraCode, "\n\t")
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

	String getImplementStmt() {
		if (!interfaces || interfaces.trim() == "") return ""
		return " implements " + interfaces
	}

	String getExtendsImplementStmt() {
		if (modelClass) {
			importType('javax.persistence.Transient')
			importType('javax.persistence.Version')
			importType('com.axelor.db.annotations.Widget')
			return ""
		}
		return " extends " + getBaseClass() + getImplementStmt()
	}

	String getAbstractStmt() {
		return modelClass ? "abstract " : ""
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

	private String stripCode(code, prefix) {
		if (!code || code.trim().empty) return "";
		return prefix + Utils.stripCode(code, prefix)
	}

	String getExtraCode() {
		return stripCode(extraCode, "\n\t");
	}

	String getExtraImports() {
		if (!extraImports || extraImports.trim().isEmpty()) return "";
		return "\n" + Utils.stripCode(extraImports, "\n") + "\n"
	}

	private List<Property> getHashables() {
		return properties.findAll { p -> p.hashKey }
	}

	boolean isModelClass() {
		return modelClass;
	}

	String getEqualsCode() {

		if (hasExtends) {
			return "return EntityHelper.equals(this, obj);"
		}

		def hashables = getHashables()
		def code = [
			"if (obj == null) return false;"
		]

		importType("java.util.Objects")

		if (groovy) {
			code += "if (this.is(obj)) return true;"
		} else {
			code += "if (this == obj) return true;"
		}
		code += "if (!(obj instanceof ${name})) return false;"
		code += ""
		code += "final ${name} other = (${name}) obj;"
		code += "if (this.getId() != null || other.getId() != null) {"
		code += "\treturn Objects.equals(this.getId(), other.getId());"
		code += "}"
		if (!hashables.empty) {
			code += ""
			code += getHashables().collect { p -> "if (!Objects.equals(${p.getter}(), other.${p.getter}())) return false;"}
		}
		code += ""
		code += hashables.empty ? "return false;" : "return true;"
		return code.join("\n\t\t")
	}

	String getHashCodeCode() {
		if (hasExtends) {
			return "return EntityHelper.hashCode(this);"
		}
		importType("java.util.Objects")
		def data = getHashables().collect { "this.${it.getter}()" }.join(", ")
		if (data.size()) {
			def hash = name.hashCode()
			return "return Objects.hash(${hash}, ${data});"
		}
		return "return 31;"
	}

	String getToStringCode() {
		if (hasExtends) {
			return "return EntityHelper.toString(this);"
		}

		importType("com.google.common.base.MoreObjects")

		def code = ""

		code += "return MoreObjects.toStringHelper(this)\n"
		code += "\t\t\t.add(\"id\", getId())\n"
		int count = 0
		for(Property p : properties) {
			if (p.virtual || p.password || p.json || !p.simple || p.name == "id" || p.name == "version") continue
				code += "\t\t\t.add(\"${p.name}\", ${p.getter}())\n"
			if (count++ == 10) break
		}
		return code + "\t\t\t.omitNullValues()\n\t\t\t.toString();"
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

		def all = []

		if (!mappedSuper) {
			all += [
				new Annotation(this, "javax.persistence.Entity", true),
				$cacheable()
			]
		}

		if (!mappedSuper && dynamicUpdate) {
			all += new Annotation(this, "org.hibernate.annotations.DynamicInsert", true)
			all += new Annotation(this, "org.hibernate.annotations.DynamicUpdate", true)
		}

		all += $table()
		all += $strategy()
		all += $track()
		all += $mappedSuperClass()
		all += $listeners()

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

	Annotation $table() {

		if (!table || mappedSuper) return null

		def constraints = this.constraints.collect {
			def idx = new Annotation(this, "javax.persistence.UniqueConstraint", false)
			if (it.name) {
				idx.add("name", it.name)
			}
			return idx.add("columnNames", it.columns, true)
		}

		def indexes = this.indexes.collect {
			def idx = new Annotation(this, "javax.persistence.Index", false)
			if (it.name) {
				idx.add("name", it.name)
			}
			return idx.add("columnList", it.columns.join(','), true)
		}

		def a1 = new Annotation(this, "javax.persistence.Table", false).add("name", this.table)
		if (!constraints.empty)
			a1.add("uniqueConstraints", constraints, false)
		if (!indexes.empty)
			a1.add("indexes", indexes, false)

		return a1
	}

	Annotation $cacheable() {
		if (cacheable == "true") {
			return new Annotation(this, "javax.persistence.Cacheable", true)
		}
		if (cacheable == "false") {
			return new Annotation(this, "javax.persistence.Cacheable", false).add("false", false)
		}
		return null
	}

	Annotation $mappedSuperClass() {
		if (mappedSuper) {
			return new Annotation(this, "javax.persistence.MappedSuperclass", true)
		}
	}

	Annotation $strategy() {
		// Inheritance strategy can be specified on root entity only
		if (!strategy || hasExtends) return null
		String type = "SINGLE_TABLE"
		if (strategy == "JOINED") type = "JOINED"
		if (strategy == "CLASS") type = "TABLE_PER_CLASS"

		return new Annotation(this, "javax.persistence.Inheritance")
				.add("strategy", "javax.persistence.InheritanceType.${type}", false)
	}

	Annotation $track() {
		if (!track) return null
		return track.$track()
	}

	Annotation $listeners() {
		if(listeners.empty) return null;
		return new Annotation(this, "javax.persistence.EntityListeners")
				.add("{  ${ listeners.collect { importType(it) + '.class' }.join(', ') } }", false)
	}

	@Override
	String toString() {
		def names = fields.collect { it.name }
		return "Entity(name: $name, fields: $names)"
	}

	public Track getTrack() {
		return track;
	}
}
