/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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

import com.axelor.common.Inflector
import com.axelor.tools.x2j.Utils

import groovy.util.slurpersupport.NodeChild

class Property {

	String name

	String type

	String code

	String target
	
	String targetFqn

	Entity entity

	Entity ownEntity

	private boolean initParam

	private Map<String, Object> attrs = new HashMap()

	private Property(Entity entity, String name, String type) {
		this.entity = entity
		this.name = name
		this.type = type
		this.code = ""
		this.target = ""
	}

	Property(Entity entity, NodeChild node) {
		this.entity = entity
		name = node.@name
		type = node.name()
		code = node.text()
		targetFqn = node.@ref.toString()
		target = entity.importType(targetFqn)
		attrs = node.attributes()
		initParam = attrs["initParam"] == "true"

		if (!name) {
			throw new IllegalArgumentException("Property name not given.")
		}
	}

	String getType() {
		switch(type) {
			case "string":
				return "String"
			case "integer":
				return "Integer"
			case "long":
				return "Long"
			case "boolean":
				return "Boolean"
			case ["double", "float"]:
				return "Double"
			case "decimal":
				return entity.importType("java.math.BigDecimal")
			case "date":
				return entity.importType("java.time.LocalDate")
			case "time":
				return entity.importType("java.time.LocalTime")
			case "datetime":
				def t = attrs['tz'] == 'true' ? 'ZonedDateTime' : 'LocalDateTime'
				return entity.importType('java.time.' + t)
			case "binary":
				return "byte[]"
			case "enum":
			case "one-to-one":
			case "many-to-one":
				return entity.importType(targetFqn)
			case "one-to-many":
				def targetName = entity.importType(targetFqn)
				return entity.importType("java.util.List<$targetName>")
			case "many-to-many":
				def targetName = entity.importType(targetFqn)
				return entity.importType("java.util.Set<$targetName>")
		}
		throw new IllegalArgumentException("Invalid type: " + type)
	}

	String getServerType() {
		return type
	}

	boolean isSimple() {
		switch(type) {
			case "string":
				if (isLarge()) return false
				return true
			case "integer":
			case "long":
			case "boolean":
			case ["double", "float"]:
			case "decimal":
			case "date":
			case "time":
			case "datetime":
			case "enum":
				return true
		}
		return false
	}

	String getValue() {

		String value = attrs['default']

		if (value == null || "" == value.trim()) {
			return this.getEmptyValue()
		}

		switch(type) {
			case "boolean":
				return value ==~ /(?i)true|t|1|Boolean\\.TRUE/ ? "Boolean.TRUE" : "Boolean.FALSE"
			case "string":
				return "\"${value}\""
			case "long":
				return value.endsWith("L") ? value : "${value}L"
			case [ "integer", "double", "boolean" ]:
				return value
			case "decimal":
				return "new BigDecimal(\"${value}\")"
			case "date":
				return value == "now" ? "LocalDate.now()" : "LocalDate.parse(\"${value}\")"
			case "time":
				return value == "now" ? "LocalTime.now()" : "LocalTime.parse(\"${value}\")"
			case "datetime":
				def t = attrs['tz'] == 'true' ? 'ZonedDateTime' : 'LocalDateTime'
				return value == "now" ? "${t}.now()" : "${t}.parse(\"${value}\")"
			case "enum":
				return entity.importType(targetFqn) + "." + value
		}
	}

	String getDefaultExpression() {
		if (name == "id" || name == "version")
			return ""
		return this.getValue() == null ? "" : " = ${value}"
	}

	String getEmptyValue() {

		if (isNullable()) {
			return null
		}

		switch(type) {
			case "boolean":
				return "Boolean.FALSE"
			case "integer":
				return "0"
			case "long" :
				return "0L"
			case "decimal":
				return "BigDecimal.ZERO"
		}
		return null
	}

	String getGetter() {
		"get" + firstUpper(name)
	}

	String getSetter() {
		"set" + firstUpper(name)
	}

	String getGetterBody() {

		if (name == "id" || name == "version") {
			return "return $name;"
		}

		def result = []
		def empty = this.getEmptyValue()

		if (empty != null) {
			return "return $name == null ? $empty : $name;"
		}
		return "return $name;"
	}

	String getSetterBody() {
		return "this.$name = $name;"
	}

	String getLinkCode() {
		def mapped = attrs["mappedBy"]
		if (!mapped || type != "one-to-many") {
			return null
		}
		return "item.set" + firstUpper(mapped) + "(this);"
	}

	String getDelinkCode() {
		def mapped = attrs["mappedBy"]
		def orphanRemoval = this.isOrphanRemoval()
		if (orphanRemoval || !mapped || type != "one-to-many") {
			return null
		}
		return "item.set" + firstUpper(mapped) + "(null);"
	}

	String getDelinkAllCode() {
		def mapped = attrs["mappedBy"]
		def orphanRemoval = this.isOrphanRemoval()
		if (orphanRemoval || !mapped || type != "one-to-many") {
			return null
		}
		return """for (${target} item : ${name}) {
				item.set${firstUpper(mapped)}(null);
			}"""
	}

	String getSingularName() {
		return getSingularName(name)
	}

	String getSingularName(String name) {
		if (name =~ /(Set|List)$/) {
			return name + "Item"
		}
		return Inflector.getInstance().singularize(name)
	}

	String getMappedBy() {
		return attrs["mappedBy"]
	}

	String getColumn() {
		String col = attrs['column']
		if (!col || col.trim().empty) {
			return null
		}
		return col
	}

	String getColumnAuto() {
		String col = getColumn()
		if (col) {
			return col
		}
		// follow hibernate naming
		final StringBuilder buf = new StringBuilder(name.replace('.', '_'));
		for (int i = 1; i < buf.length() - 1; i++) {
			if (
				Character.isLowerCase(buf.charAt(i-1)) &&
				Character.isUpperCase(buf.charAt(i) ) &&
				Character.isLowerCase(buf.charAt(i+1))
			) {
				buf.insert(i++, '_');
			}
		}
		return buf.toString().toLowerCase()
	}

	boolean isInitParam() {
		return !ownEntity && initParam
	}

	boolean isNullable() {
		return attrs["nullable"] == "true" && attrs["required"] != "true"
	}

	boolean isOrphanRemoval() {
		if (attrs.containsKey("orphanRemoval")) {
			return attrs["orphanRemoval"] == "true"
		}
		// if still using old "orphan" attribute
		if (attrs.containsKey("orphan")) {
			return attrs["orphan"] == "false"
		}
		return type == "one-to-many" && attrs["mappedBy"] != null
	}
	
	boolean isEnum() {
		return type == 'enum'
	}

	boolean isJson() {
		return attrs["json"] == "true"
	}

	boolean isPassword() {
		return attrs["password"] == "true"
	}

	boolean isUnique() {
		return attrs["unique"] == "true"
	}
	
	boolean isSequence() {
		return attrs["sequence"]
	}

	boolean isHashKey() {
		if (name == "id" || name == "version") return false
		if (attrs["hashKey"] == "false") return false
		if (attrs["hashKey"] == "true" || isUnique()) return true
		return entity.hashAll && isSimple() && !isVirtual()
	}

	Object getAttribute(String name) {
		return attrs[name]
	}

	String newCollection() {
		if (type == "many-to-many") {
			importName("java.util.HashSet")
			return "new HashSet<$target>()"
		}
		importName("java.util.ArrayList")
		return "new ArrayList<$target>()"
	}

	String firstUpper(String string) {
		return Utils.firstUpper(string)
	}

	String getCode() {
		return Utils.stripCode(this.code, "\n\t\t")
	}

	String getFormula() {
		String text = this.code
		if (text == null) {
			return ""
		}
		text = text.replaceAll("\"", '''\\\\"''')
		text = Utils.stripCode(text, "\n\t\t\"")

		text = "\"(" + text.replaceAll("\n", "\" +\n") + ")\""

		if (text.indexOf('\n') != text.lastIndexOf('\n')) {
			text = "\n\t\t" + text
		}

		return text
	}

	String getDocumentation() {
		String text = Utils.stripCode(attrs.get("help"), "\n * ")
		if (text == "") {
			return ""
		}
		return """
\t/**
\t * """ + text + """
\t *
\t * @return the property value
\t */"""
	}

	String importName(String qname) {
		entity.importType(qname)
	}

	boolean isLarge() {
		return attrs['large'] == 'true'
	}

	boolean isReference() {
		type == "many-to-one" || type == "one-to-one"
	}

	boolean isCollection() {
		type == "one-to-many" || type == "many-to-many"
	}

	boolean isTransient() {
		return attrs["transient"] == "true"
	}

	boolean isNameField() {
		return attrs.namecolumn == "true"
	}

	boolean isVirtual() {
		return code != null && code.trim().length() > 0
	}

	boolean isFormula() {
		return attrs.formula == 'true' && !isCollection()
	}

	boolean isIndexable() {
		if (this.isUnique() || this.isFormula() || this.isTransient() || attrs['index'] == 'false')
			return false
		String index = attrs['index'] as String
		return index =~ /true|^idx_/ ||
			attrs['namecolumn'] == 'true' ||
			name in ['name', 'code'] ||
			this.isReference() && !attrs['mappedBy']
	}

	static Property idProperty(Entity entity) {
		new Property(entity, "id", "long")
	}

	List<Annotation> getAnnotations() {
		[
			$id(),
			$hashKey(),
			$widget(),
			$binary(),
			$nameColumn(),
			$virtual(),
			$required(),
			$size(),
			$digits(),
			$transient(),
			$column(),
			$one2one(),
			$many2one(),
			$one2many(),
			$many2many(),
			$joinTable(),
			$orderBy(),
			$sequence()
		]
		.grep { it != null }
		.flatten()
		.grep { Annotation a ->
			!a.empty
		}
	}

	private Annotation annon(String name) {
		return annon(name, false)
	}

	private Annotation annon(String name, boolean empty) {
		return new Annotation(entity, name, empty)
	}

	private Annotation $column() {

		def column = attrs.column
		def unique = attrs.unique

		if (Naming.isReserved(name)) {
			throw new IllegalArgumentException(
				"Invalid use of a Java keyword '${name}' in domain object: ${entity.name}")
		}

		if (!collection) {
			def col = getColumnAuto()
			if (Naming.isKeyword(col)) {
				throw new IllegalArgumentException(
					"Invalid use of an SQL keyword '${col}' in domain object: ${entity.name}")
			}
		}

		if (column == null && unique == null)
			return null

		annon(reference ? "javax.persistence.JoinColumn" : "javax.persistence.Column")
				.add("name", column)
				.add("unique", unique, false)
	}
	
	private Annotation $joinTable() {
		
		def joinTable = attrs.table
		def fk1 = attrs.column
		def fk2 = attrs.column2

		if (joinTable == null)
			return null

		def res = annon("javax.persistence.JoinTable").add("name", joinTable)

		if (fk1) res.add("joinColumns", annon("javax.persistence.JoinColumn").add("name", fk1, true).toString(), false)
		if (fk2) res.add("inverseJoinColumns", annon("javax.persistence.JoinColumn").add("name", fk2, true).toString(), false)

		return res
	}

	private Annotation $transient() {
		if (isTransient()) {
			return annon("javax.persistence.Transient", true)
		}
	}

	private List<Annotation> $size() {

		def min = attrs.min
		def max = attrs.max

		if (min == null && max == null)
			return null

		def all = []

		switch (type) {
			case "decimal":
				if (min != null) all += annon("javax.validation.constraints.DecimalMin").add(min)
				if (max != null) all += annon("javax.validation.constraints.DecimalMax").add(max)
				return all
			case "string":
				return [
					annon("javax.validation.constraints.Size")
					.add("min", min, false)
					.add("max", max, false)
				]
		}

		if (min != null) all += annon("javax.validation.constraints.Min").add(min, false)
		if (max != null) all += annon("javax.validation.constraints.Max").add(max, false)

		return all
	}

	private Annotation $digits() {

		def precision = attrs['precision']
		def scale = attrs['scale']

		if (precision == null && scale == null)
			return null

		precision = precision as Integer
		scale = scale as Integer

		annon("javax.validation.constraints.Digits", false)
				.add("integer", (precision - scale) as String, false)
				.add("fraction", scale as String, false)
	}

	private Annotation $required() {
		if (attrs.required == "true")
			annon("javax.validation.constraints.NotNull", true)
	}

	private List<Annotation> $virtual() {
		if (!this.isVirtual()) {
			return null
		}
		def all = [annon("com.axelor.db.annotations.VirtualColumn", true)]
		if (this.isTransient()) {
			return all
		}

		if (this.isFormula()) {
			all += [annon(reference ? "org.hibernate.annotations.JoinFormula" : "org.hibernate.annotations.Formula")
							.add(this.getFormula(), false)]
		} else {
			all += [annon("javax.persistence.Access").add(
						entity.importType("javax.persistence.AccessType.PROPERTY"), false)]
		}
		return all
	}

	private Annotation $nameColumn() {
		if (!entity && isNameField())
			return annon("com.axelor.db.annotations.NameColumn", true)
		if (entity.nameField == this || (!entity.nameField && isNameField()))
			annon("com.axelor.db.annotations.NameColumn", true)
	}

	private Annotation $widget() {

		def title = attrs['title']
		def help = attrs['help']
		def readonly = attrs['readonly']
		def hidden = attrs['hidden']
		def search = attrs['search']
		def multiline = attrs['multiline']
		def selection = attrs['selection']
		def image = attrs['image']
		def password = attrs['password']
		def massUpdate = attrs['massUpdate']
		def translatable = attrs['translatable']
		def copyable = attrs['copy']
		def defaultNow = attrs['default'] == 'now';

		if (massUpdate && (isUnique() || isCollection() || isLarge())) {
			massUpdate = false;
		}

		if (selection) {
			selection = selection.replaceAll("\\],\\s*\\[", '], [')
		}

		if (title || help || readonly || hidden || multiline || selection ||
			image || isPassword() || massUpdate || search || translatable || copyable || defaultNow)
			annon("com.axelor.db.annotations.Widget")
				.add("image", image, false)
				.add("title", title)
				.add("help", help)
				.add("readonly", readonly, false)
				.add("hidden", hidden, false)
				.add("multiline", multiline, false)
				.add("search", search, true, true)
				.add("selection", selection)
				.add("password", password, false)
				.add("massUpdate", massUpdate, false)
				.add("translatable", translatable, false)
				.add("copyable", copyable, false)
				.add("defaultNow", defaultNow ? "true" : null, false)
	}

	private List<Annotation> $binary() {
		
		if (isJson() && type == 'string') {
			return [
				annon("org.hibernate.annotations.Type").add("type", "json")
			]
		}
		
		if (isEnum()) {
			return [
				annon("javax.persistence.Basic", true),
				annon("org.hibernate.annotations.Type").add("type", "com.axelor.db.hibernate.type.ValueEnumType")
			]
		}

		if (isLarge() && type == 'string') {
			return [
				annon("javax.persistence.Lob", true),
				annon("javax.persistence.Basic").add("fetch", "javax.persistence.FetchType.LAZY", false),
				annon("org.hibernate.annotations.Type").add("type", "text")
			]
		}

		if (isLarge() || type == 'binary') {
			return [
				annon("javax.persistence.Lob", true),
				annon("javax.persistence.Basic").add("fetch", "javax.persistence.FetchType.LAZY", false)
			]
		}
	}

	private Index getIndex() {
		if (!this.isIndexable()) return null
		String index = attrs['index'] as String
		if (!index || index == 'true' || index.trim().empty)
			index = null
		return new Index(this.entity, index, [this.name])
	}

	private List<Annotation> $id() {

		if (name != "id")
			return null

		if (!entity.sequential || entity.mappedSuper) {
			return [
				annon("javax.persistence.Id", true),
				annon("javax.persistence.GeneratedValue")
					.add("strategy", "javax.persistence.GenerationType.AUTO", false)
			]
		}

		def name = entity.table + '_SEQ'

		[
			annon("javax.persistence.Id", true),
			annon("javax.persistence.GeneratedValue")
				.add("strategy", "javax.persistence.GenerationType.SEQUENCE", false)
				.add("generator", name),
			annon("javax.persistence.SequenceGenerator")
				.add("name", name)
				.add("sequenceName", name)
				.add("allocationSize", "1", false)
		]
	}

	private Annotation $one2one() {
		if (type != "one-to-one") return null

		def mapped = attrs.get('mappedBy')
		def orphanRemoval = this.isOrphanRemoval()

		def a = annon("javax.persistence.OneToOne")
			.add("fetch", "javax.persistence.FetchType.LAZY", false)
			.add("mappedBy", mapped)

		if (orphanRemoval) {
			a.add("cascade", "javax.persistence.CascadeType.ALL", false)
			a.add("orphanRemoval", "true", false)
		} else {
			a.add("cascade", ["javax.persistence.CascadeType.PERSIST","javax.persistence.CascadeType.MERGE"], false)
		}
		return a
	}

	private Annotation $many2one() {
		if (type != "many-to-one") return null

		annon("javax.persistence.ManyToOne")
			.add("fetch", "javax.persistence.FetchType.LAZY", false)
			.add("cascade", ["javax.persistence.CascadeType.PERSIST", "javax.persistence.CascadeType.MERGE"], false)
	}

	private Annotation $one2many() {

		if (type != "one-to-many") return null

		def mapped = attrs.get('mappedBy')
		def orphanRemoval = this.isOrphanRemoval()

		def a = annon("javax.persistence.OneToMany")
			.add("fetch", "javax.persistence.FetchType.LAZY", false)
			.add("mappedBy", mapped)

		if (orphanRemoval) {
			a.add("cascade", "javax.persistence.CascadeType.ALL", false)
			a.add("orphanRemoval", "true", false)
		} else {
			a.add("cascade", ["javax.persistence.CascadeType.PERSIST", "javax.persistence.CascadeType.MERGE"], false)
		}
		return a
	}

	private Annotation $many2many() {
		def mapped = attrs.get('mappedBy')
		if (type == "many-to-many")
			annon("javax.persistence.ManyToMany")
				.add("fetch", "javax.persistence.FetchType.LAZY", false)
				.add("mappedBy", mapped)
				.add("cascade", ["javax.persistence.CascadeType.PERSIST", "javax.persistence.CascadeType.MERGE"], false)
	}

	private Annotation $orderBy() {
		def orderBy = attrs.get('orderBy')?.trim()
		if (!orderBy) return null

		orderBy = orderBy.split(/,/).collect {
			it.trim().replaceAll(/-\s*(\w+)/, '$1 DESC')
		}.join(", ")

		return annon("javax.persistence.OrderBy").add(orderBy)
	}
	
	private Annotation $sequence() {
		def sequence = attrs.get('sequence')?.trim()
		if (!sequence) return null
		return annon("com.axelor.db.annotations.Sequence").add(sequence);
	}

	private Annotation $hashKey() {
		if (!hashKey) return null
		return annon("com.axelor.db.annotations.HashKey", true)
	}
}
