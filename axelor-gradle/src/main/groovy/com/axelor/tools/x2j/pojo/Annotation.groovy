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

class Annotation {

	String name

	List<String> args = []

	boolean empty

	Entity entity

	ImportManager importManager

	Annotation(Entity entity, String name) {
		this(entity, name, false)
	}

	Annotation(Entity entity, String name, boolean empty) {
		this(entity.importManager, name, empty)
		this.entity = entity
	}

	Annotation(ImportManager importer, String name, boolean empty) {
		this.importManager = importer
		this.name = "@" + importer.importType(name)
		this.empty = empty
	}

	boolean isEmpty() {
		args.empty && !empty
	}

	String quote(String s) {
		if (!s?.startsWith('"'))
			return '"' + s + '"'
		return s
	}

	String wrap(List<String> names) {
		def value = names.join(", ")
		entity.groovy ? "[ $value ]" : "{ $value }"
	}

	Annotation add(String value) {
		this.add(value, true)
	}

	Annotation add(String value, boolean quote) {
		if (value != null)
			args.add(quote ? this.quote(value) : value)
		this
	}

	Annotation add(String param, String value) {
		this.add(param, value, true)
	}

	Annotation add(String param, String value, boolean quote) {
		if (value == null)
			return this
		this.add(param, [value], quote, true)
	}

	Annotation add(String param, String value, boolean quote, boolean array) {
		if (value == null)
			return this
		def values = array ? value.split(/,/) as List : [value]
		this.add(param, values, quote, ! array)
	}

	Annotation add(String param, List<?> values, boolean quote) {
		this.add(param, values, quote, false)
	}

	Annotation add(String param, List<?> values, boolean quote, boolean unwrapSingle) {
		if (values == null)
			return this;

		values = values.collect {
			if (it instanceof Annotation)
				return it
			quote ? this.quote(it) : importManager.importType(it)
		}

		def value = unwrapSingle && values.size() == 1 ? values[0] : wrap(values)

		args.add("$param = $value")
		return this
	}

	@Override
	String toString() {
		if (args.empty)
			return name
		"${name}(${args.join(', ')})"
	}
}
