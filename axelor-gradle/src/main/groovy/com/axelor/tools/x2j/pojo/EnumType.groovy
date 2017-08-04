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

	List<EnumItem> items
	
	private ImportManager importManager
	
	transient long lastModified

	EnumType(NodeChild node) {
		name = node.@name
		numeric = node.@integer == 'true'
		module = node.parent().module.'@name'
		namespace = node.parent().module."@package"
		importManager = new ImportManager(namespace, false)
		documentation = findDocs(node)
		items = node."item".collect { new EnumItem(this, it) }
	}
	
	void validate() {
		def map = [:]
		def dup = [] as Set
		for (int i = 0; i < items.size(); i++) {
			def item = items[i];
			def key = item.value?:null
			if (numeric && (key == null || Ints.tryParse(key) == null)) {
				throw new IllegalArgumentException("Invalid enum '${name}', expects item '${item.name}' with integer value.")
			}
			if (key == null) {
				key = item.name
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
		numeric ? 'Integer' : 'String'
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
