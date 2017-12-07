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

import com.axelor.tools.x2j.Utils
import com.google.common.primitives.Ints

import groovy.util.slurpersupport.NodeChild

class EnumType {

	String name

	String module

	String namespace
	
	String documentation
	
	Boolean numeric
	
	Boolean valueEnum

	List<EnumItem> items
	
	Map<String, Property> itemsMap

	EnumType baseType
	
	private ImportManager importManager
	
	transient long lastModified

	EnumType(NodeChild node) {
		name = node.@name
		numeric = node.@numeric == 'true'
		module = node.parent().module.'@name'
		namespace = node.parent().module."@package"
		importManager = new ImportManager(namespace, false)
		documentation = findDocs(node)
		
		itemsMap = [:]
		items = []
		
		node."item".each {
			EnumItem item = new EnumItem(this, it)
			itemsMap[item.name] = item
			items.add(item)
			if (item.value) {
				valueEnum = true
			}
		}
	}

	private boolean isCompatible(EnumItem existing, EnumItem item) {
		if (existing == null) return true
		if (existing.name != item.name) return false
		return true
	}

	void merge(EnumType other) {
		for (EnumItem item : other.items) {
			EnumItem existing = itemsMap.get(item.name)
			if (isCompatible(existing, item)) {
				item.entity = this
				if (existing != null) {
					items.remove(existing)
				}
				items.add(item)
				itemsMap[item.name] = item
			}
		}
		other.baseType = this
	}
	
	void validate() {
		def map = [:]
		def dup = [] as Set
		for (int i = 0; i < items.size(); i++) {
			def item = items[i];
			def key = item.value?:null
			if (valueEnum || numeric) {
				if (key == null) {
					throw new RuntimeException("Invalid enum '${name}', expects item '${item.name}' with a value.")
				}
				if (numeric && Ints.tryParse(key) == null) {
					throw new IllegalArgumentException("Invalid enum '${name}', expects item '${item.name}' with numeric value.")
				}
			}
			if (key == null) {
				key = item.name
			}
			if (key == null) {
				throw new IllegalArgumentException("Invalid enum '${name}', expects item")
			}
			if (map.containsKey(key)) {
				dup.add(map.get(key))
				dup.add(item)
			} else {
				map.put(key, item)
			}
		}
		if (!dup.empty) {
			def names = dup.collect { it.name }
			throw new IllegalArgumentException("Invalid enum '${name}', duplicate items: ${names}")
		}
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
		String text = Utils.stripCode(documentation, '\n * ')
		if (text == "") {
			return ""
		}
		return """
/**
 * """ + text + """
 */"""
	}
	
	List<String> getImportStatements() {
		return importManager.getImportStatements()
	}
	
	String getFile() {
		namespace.replace(".", "/") + "/" + name + ".java";
	}
	
	String getType() {
		return numeric ? 'Integer' : 'String';
	}
	
	String getImplementsCode() {
		if (numeric || valueEnum) {
			importManager.importType("java.util.Objects")
		}
		importManager.importType("com.axelor.db.ValueEnum")
		return "implements ValueEnum<${type}> "
	}

	public List<EnumItem> getItems() {
		validate()
		return items;
	}

	@Override
	String toString() {
		def names = items.collect { it.name }
		return "Enum(name: $name, items: $names)"
	}
}
