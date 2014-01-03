/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
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
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.tools.x2j.pojo

class ImportManager {

	private String base

	private Set<String> imports = new HashSet()

	private Map<String, String> names = new HashMap()

	private boolean groovy

	def GROOVY_PAT = ~/(java\.(io|net|lang|util))|(groovy\.(lang|util))/

	ImportManager(String base, boolean groovy) {
		this.base = base
		this.groovy = groovy
	}

	String importType(String fqn) {

		List<String> parts = fqn.split("\\.")
		if (parts.size() == 1)
			return fqn

		String simpleName = parts.pop()
		def name = simpleName

		if (name ==~ /[A-Z_]+/ && parts.last() ==~ /[A-Z_].*/) {
			simpleName = parts.pop()
			name = simpleName + "." + name
		}

		simpleName = simpleName.replaceAll("\\<.*", "")

		def pkg = parts.join(".")

		if (pkg == base || pkg == "java.lang") {
			return name
		}

		if (groovy && (pkg ==~ GROOVY_PAT || simpleName == 'BigDecimal')) {
			return name
		}

		def newFqn = pkg + "." + simpleName
		def canBeSimple = true

		if (names.containsKey(simpleName)) {
			def existing = names.get(simpleName)
			canBeSimple = existing == newFqn
		} else {
			names.put(simpleName, newFqn)
			imports.add(newFqn)
		}

		if (canBeSimple)
			return name
		return fqn
	}

	List<String> getImports() {
		return new ArrayList<String>(imports).sort()
	}

	List<String> getImportStatements() {

		def all = new ArrayList<String>()
		def groups = imports.groupBy {
			it.split('\\.')[0]
		}

		def coms = groups.remove("com")

		try {
			all.addAll(groups.remove("java").sort())
			all.add(null)
		} catch (NullPointerException e){}

		try {
			all.addAll(groups.remove("javax").sort())
			all.add(null)
		} catch (NullPointerException e){}

		groups.each {
			all.addAll(it.value.sort())
			all.add(null)
		}

		if (coms) {
			all.addAll(coms.sort())
			all.add(null)
		}

		all.pop()

		return all.collect {
			it == null ? "" : "import " + it + ";"
		}
	}
}
