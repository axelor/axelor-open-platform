package com.axelor.tool.x2j.pojo

import groovy.util.slurpersupport.GPathResult;
import groovy.util.slurpersupport.NodeChild;

import com.google.common.base.CaseFormat;

class Entity {

	String name

	String table
	
	String module

	String namespace
	
	String baseClass

	boolean sequential
	
	boolean groovy
	
	String documentation;

	List<Property> properties
	
	List<Constraint> constraints
	
	private ImportManager importManager
	
	Entity(NodeChild node) {
		name = node.@name
		table = node.@table
		namespace = node.parent().module."@package"
		module = node.parent().module.@name
		sequential = node.@sequential == "true"
		groovy = node.@lang == "groovy"
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
		
		node."*".each {
			if (it.name() == "unique-constraint")
				constraints += new Constraint(this, it)
			else
				properties += new Property(this, it)
		}
		
		Property idp = Property.idProperty(this)
		properties = [idp] + properties
		
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
		if (documentation == null || documentation.trim() == "")
			return "";
			
		String text = documentation.trim().replaceAll('    ', '\t')
		text = text.stripIndent().replaceAll(/\n/, '\n * ')
		
		return """
/**
 * """ + text + """
 */"""
	}
	
	String importType(String fqn) {
		return importManager.importType(fqn)
	}
	
	List<String> getImports() {
		return importManager.getImports()
	}
	
	List<Annotation> getAnnotations() {
		[
			new Annotation(this, "javax.persistence.Entity", true),
			$table()
		]
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
}
