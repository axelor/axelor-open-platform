/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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


class Repository {

	private Entity entity

	private ImportManager importManager

	String namespace
	String baseClass
	String modelClass

	String name

	boolean concrete = true

	public Repository(Entity entity) {

		this.entity = entity

		this.namespace = entity.repoNamespace
		this.baseClass = "JpaRepository<${entity.name}>"
		this.modelClass = entity.name
		this.name = "${entity.name}Repository"

		this.importManager = new ImportManager(this.namespace, false)

		this.importManager.importType("com.axelor.db.JpaRepository")
		this.importManager.importType("${entity.namespace}.${entity.name}")
	}

	String getClassName() {
		if (concrete) {
			return name;
		}
		return "Abstract${name}"
	}

	String getClassStatement() {
		if (concrete) {
			return "class $className"
		}
		return "abstract class $className"
	}

	String getFile() {
		return namespace.replace(".", "/") + "/${className}.java"
	}

	String importType(String fqn) {
		return importManager.importType(fqn)
	}

	List<String> getImportStatements() {
		return importManager.getImportStatements()
	}

	List<Finder> getFinderMethods() {
		return this.entity.getFinderMethods()
	}

	String getExtraImports() {
		this.entity.extraImports
	}

	String getExtraCode() {
		this.entity.extraCode
	}
}
